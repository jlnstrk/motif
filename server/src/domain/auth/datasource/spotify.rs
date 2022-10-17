use std::env;

use chrono::{Duration, Utc};
use oauth2::basic::BasicClient;
use oauth2::reqwest::async_http_client;
use oauth2::{
    AuthUrl, AuthorizationCode, ClientId, ClientSecret, CsrfToken, RedirectUrl, RefreshToken,
    Scope, TokenResponse, TokenUrl,
};
use reqwest::{header, Method};
use sea_orm::DatabaseConnection;
use serde::Deserialize;
use uuid::Uuid;

use crate::domain::auth::datasource::{service, token};
use crate::domain::auth::dto::{ServiceAccountDto, ServiceCredentialsDto, ServiceLoginDto};
use crate::domain::auth::typedef::{AuthToken, AuthTokenWithRefresh, ServiceToken};
use crate::domain::common::typedef::Service;
use crate::rest::auth::CallbackMode;
use crate::rest::util::{ApiError, AuthenticationError};

struct SpotifyEnv {
    client_id: String,
    client_secret: String,
    redirect_uri: String,
    redirect_uri_mobile: String,
}

fn get_env() -> SpotifyEnv {
    SpotifyEnv {
        client_id: env::var("SPOTIFY_CLIENT_ID").expect("SPOTIFY_CLIENT_ID must be set"),
        client_secret: env::var("SPOTIFY_CLIENT_SECRET")
            .expect("SPOTIFY_CLIENT_SECRET must be set"),
        redirect_uri: env::var("SPOTIFY_REDIRECT_URI").expect("SPOTIFY_REDIRECT_URI must be set"),
        redirect_uri_mobile: env::var("SPOTIFY_REDIRECT_URI_MOBILE")
            .expect("SPOTIFY_REDIRECT_URI_MOBILE must be set"),
    }
}

fn oauth_client(callback_mode: Option<CallbackMode>) -> BasicClient {
    let spotify_env = get_env();
    let redirect_uri = match callback_mode {
        Some(CallbackMode::Server) => spotify_env.redirect_uri,
        _ => spotify_env.redirect_uri_mobile,
    };
    BasicClient::new(
        ClientId::new(spotify_env.client_id),
        Some(ClientSecret::new(spotify_env.client_secret)),
        AuthUrl::new("https://accounts.spotify.com/authorize".to_string()).unwrap(),
        Some(TokenUrl::new("https://accounts.spotify.com/api/token".to_string()).unwrap()),
    )
    .set_redirect_uri(RedirectUrl::new(redirect_uri).unwrap())
}

pub(crate) fn auth_url(callback_mode: CallbackMode) -> String {
    let client = oauth_client(Some(callback_mode));
    let (url, ..) = client
        .authorize_url(CsrfToken::new_random)
        .add_scope(Scope::new("user-read-email".to_owned()))
        .add_scope(Scope::new("user-read-private".to_owned()))
        .url();
    url.to_string()
}

#[derive(Deserialize, Debug)]
struct SpotifyImage {
    url: String,
}

#[derive(Deserialize, Debug)]
struct SpotifyMe {
    display_name: String,
    email: String,
    id: String,
    images: Vec<SpotifyImage>,
}

pub async fn login_from_auth_code(
    db: &DatabaseConnection,
    callback_mode: CallbackMode,
    code: String,
) -> Result<(AuthTokenWithRefresh, ServiceToken<AuthToken>), ApiError> {
    let client = oauth_client(Some(callback_mode));
    let token_result = client
        .exchange_code(AuthorizationCode::new(code))
        .request_async(async_http_client)
        .await
        .unwrap();
    let access_token = token_result.access_token();
    let access_token_expires_at = token_result
        .expires_in()
        .and_then(|dur| Utc::now().checked_add_signed(Duration::from_std(dur).unwrap()));
    let refresh_token = token_result
        .refresh_token()
        .ok_or(AuthenticationError::OAuthUnknown(
            "No refresh token returned from Spotify auth code verification".to_owned(),
        ))?;

    let client = reqwest::Client::new();
    let request = client
        .request(Method::GET, "https://api.spotify.com/v1/me")
        .bearer_auth(access_token.secret())
        .header(header::CONTENT_TYPE, "application/json")
        .build()
        .unwrap();
    let me: SpotifyMe = client.execute(request).await.unwrap().json().await.unwrap();

    let credentials_dto = ServiceCredentialsDto {
        service: Service::Spotify,
        service_id: me.id,
        access_token: access_token.secret().to_owned(),
        access_token_expires: access_token_expires_at,
        refresh_token: refresh_token.secret().to_owned(),
        refresh_token_expires: None,
    };
    let login_dto = ServiceLoginDto {
        credentials: credentials_dto,
        account: Some(ServiceAccountDto {
            email: me.email,
            display_name: me.display_name,
            photo_url: me.images.first().map(|img| img.url.clone()),
        }),
    };

    let (user_id, service_token) = service::upsert_from_service_login(&db, login_dto).await?;

    let auth_token = token::issue_jwt_pair(db, user_id).await?;

    Ok((auth_token, service_token.into_without_refresh()))
}

pub async fn refresh_from_user_id(
    db: &DatabaseConnection,
    user_id: Uuid,
) -> Result<ServiceToken<AuthToken>, ApiError> {
    let credentials = service::get_credentials_by_user_id(&db, user_id, Service::Spotify)
        .await
        .map_err(|_| AuthenticationError::OAuthRefreshTokenMissing)?;

    let client = oauth_client(None);
    let token_response = client
        .exchange_refresh_token(&RefreshToken::new(credentials.token.refresh_token))
        .request_async(async_http_client)
        .await
        .map_err(|err| AuthenticationError::OAuthUnknown(err.to_string()))?;

    let credentials_dto = ServiceCredentialsDto {
        service: Service::Spotify,
        service_id: credentials.service_id,
        access_token: token_response.access_token().secret().to_owned(),
        access_token_expires: token_response
            .expires_in()
            .and_then(|dur| Utc::now().checked_add_signed(Duration::from_std(dur).unwrap())),
        refresh_token: token_response.refresh_token().unwrap().secret().to_owned(),
        refresh_token_expires: None,
    };
    let service_token = service::update_credentials(&db, &credentials_dto).await?;
    Ok(service_token.into_without_refresh())
}
