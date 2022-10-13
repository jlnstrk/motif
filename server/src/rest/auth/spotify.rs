use std::any::Any;
use std::collections::HashMap;
use std::env;
use std::error::Error;
use std::sync::Arc;

use axum::extract::Query;
use axum::http::StatusCode;
use axum::middleware::from_fn;
use axum::response::{IntoResponse, Redirect};
use axum::routing::{get, post};
use axum::{BoxError, Extension, Json, Router};
use rspotify::clients::{BaseClient, OAuthClient};
use rspotify::model::PrivateUser;
use rspotify::{scopes, AuthCodeSpotify, Credentials, OAuth, Token};
use sea_orm::DatabaseConnection;

use crate::domain::auth;
use crate::domain::auth::dto::{ServiceAccountDto, ServiceCredentialsDto, ServiceLoginDto};
use crate::domain::auth::typedef::ServiceToken::Full;
use crate::domain::auth::typedef::{
    ErrorResponse, FullServiceToken, IntoAccessOnly, ServiceToken, ServiceTokenResponse,
};
use crate::domain::common::typedef::Service;
use crate::gql::util::AuthClaims;
use crate::rest::auth::util::verify_jwt_middleware;
use crate::rest::util::{ApiError, AuthError};

trait IntoServiceCredentials {
    fn into_service_credentials(self, service_id: String) -> ServiceCredentialsDto;
}

impl IntoServiceCredentials for Token {
    fn into_service_credentials(self, service_id: String) -> ServiceCredentialsDto {
        ServiceCredentialsDto {
            service: Service::Spotify,
            service_id,
            access_token: self.access_token,
            access_token_expires: self.expires_at,
            refresh_token: self.refresh_token.unwrap(),
            refresh_token_expires: None,
        }
    }
}

impl From<FullServiceToken> for Token {
    fn from(full: FullServiceToken) -> Self {
        Self {
            access_token: full.access_token,
            expires_in: chrono::Duration::seconds(0),
            expires_at: full.access_token_expires,
            refresh_token: Some(full.refresh_token),
            scopes: scopes!("user-read-email", "user-read-private"),
        }
    }
}

pub fn spotify_router() -> Router {
    Router::new()
        .route("/", get(auth))
        .route("/callback", get(callback))
        .route(
            "/refresh",
            post(refresh).layer(from_fn(verify_jwt_middleware)),
        )
}

pub fn make_spotify_client() -> AuthCodeSpotify {
    let client_id = env::var("SPOTIFY_CLIENT_ID").expect("SPOTIFY_CLIENT_ID must be set");
    let client_secret =
        env::var("SPOTIFY_CLIENT_SECRET").expect("SPOTIFY_CLIENT_SECRET must be set");
    let credentials = Credentials::new(&client_id, &client_secret);

    let redirect_uri = env::var("SPOTIFY_REDIRECT_URI").expect("SPOTIFY_REDIRECT_URI must be set");
    let oauth = OAuth {
        redirect_uri,
        scopes: scopes!("user-read-email", "user-read-private"),
        ..Default::default()
    };

    AuthCodeSpotify::new(credentials, oauth)
}

async fn auth() -> impl IntoResponse {
    let spotify = make_spotify_client();
    let redirect_uri = spotify.get_authorize_url(true).unwrap();
    Redirect::to(&redirect_uri)
}

async fn callback(
    Query(params): Query<HashMap<String, String>>,
    Extension(db): Extension<DatabaseConnection>,
) -> Result<Json<ServiceTokenResponse>, ApiError> {
    let code = params
        .get("code")
        .ok_or_else(|| AuthError::OAuthBadCallback)?;

    let mut spotify = make_spotify_client();
    spotify
        .request_token(&code)
        .await
        .map_err(|_| AuthError::OAuthInternal)?;

    let spotify_token: Token;
    {
        spotify_token = spotify
            .token
            .lock()
            .await
            .map_err(|_| AuthError::OAuthInternal)?
            .as_ref()
            .ok_or(AuthError::OAuthInternal)?
            .clone();
    }

    let spotify_user: PrivateUser = spotify.me().await.unwrap();

    let login_dto = ServiceLoginDto {
        credentials: spotify_token.into_service_credentials(spotify_user.id.to_string()),
        account: ServiceAccountDto {
            email: spotify_user
                .email
                .ok_or(AuthError::OAuthInsufficientClaims)?,
            display_name: spotify_user
                .display_name
                .ok_or(AuthError::OAuthInsufficientClaims)?,
            photo_url: spotify_user
                .images
                .and_then(|vec| vec.first().map(|img| img.url.clone())),
        },
    };

    let response: ServiceTokenResponse =
        auth::datasource::upsert_from_service_login(&db, login_dto)
            .await
            .map_err(|_| AuthError::OAuthInternal)?
            .into_access_only()
            .into();
    Ok(Json(response))
}

async fn refresh(
    Extension(db): Extension<DatabaseConnection>,
    Extension(claims): Extension<AuthClaims>,
) -> Result<Json<ServiceTokenResponse>, ApiError> {
    let credentials =
        auth::datasource::get_credentials_by_user_and_service(&db, claims.id, Service::Spotify)
            .await
            .map_err(|_| AuthError::OAuthRefreshTokenMissing)?;
    let token: Token = credentials.clone().into();
    let spotify = make_spotify_client();
    {
        let mut guard = spotify
            .token
            .lock()
            .await
            .map_err(|_| AuthError::OAuthInternal)?;
        *guard = Some(token);
    }

    spotify
        .refresh_token()
        .await
        .map_err(|_| AuthError::OAuthInternal)?;

    let spotify_token;
    {
        spotify_token = spotify
            .token
            .lock()
            .await
            .map_err(|_| AuthError::OAuthInternal)?
            .as_ref()
            .ok_or(AuthError::OAuthInternal)?
            .clone();
    }

    let response: ServiceTokenResponse = auth::datasource::update_from_credentials(
        &db,
        &spotify_token.into_service_credentials(credentials.service_id),
    )
    .await
    .map_err(|_| AuthError::OAuthInternal)?
    .into_access_only()
    .into();
    Ok(Json(response))
}
