use std::env;

use chrono::{Duration, Utc};
use jsonwebtoken::{Algorithm, EncodingKey, Header};
use oauth2::{
    AuthorizationCode, ClientId, ClientSecret, CsrfToken, RedirectUrl, RefreshToken, TokenResponse,
};
use openidconnect::core::{
    CoreAuthenticationFlow, CoreClient, CoreProviderMetadata, CoreResponseMode, CoreTokenResponse,
};
use openidconnect::reqwest::async_http_client;
use openidconnect::{IssuerUrl, Nonce, NonceVerifier, Scope, TokenResponse as OidcTokenResponse};
use sea_orm::DatabaseConnection;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::domain::auth::datasource::{service, token};
use crate::domain::auth::dto::{ServiceAccountDto, ServiceCredentialsDto, ServiceLoginDto};
use crate::domain::auth::typedef::{AuthToken, AuthTokenWithRefresh, ServiceToken};
use crate::domain::common::typedef::Service;
use crate::rest::auth::apple::AppleUser;
use crate::rest::auth::CallbackMode;
use crate::rest::util::{ApiError, AuthenticationError};

#[derive(Clone)]
pub struct AppleEnv {
    client_id: String,
    team_id: String,
    key_id: String,
    key_value: String,
    redirect_uri: String,
    pub redirect_uri_mobile: String,
}

pub fn get_env() -> AppleEnv {
    AppleEnv {
        client_id: env::var("APPLE_CLIENT_ID").expect("APPLE_CLIENT_ID must be set"),
        team_id: env::var("APPLE_TEAM_ID").expect("APPLE_TEAM_ID must be set"),
        key_id: env::var("APPLE_KEY_ID").expect("APPLE_KEY_ID must be set"),
        key_value: env::var("APPLE_KEY_VALUE").expect("APPLE_KEY_VALUE must be set"),
        redirect_uri: env::var("APPLE_REDIRECT_URI").expect("APPLE_REDIRECT_URI must be set"),
        redirect_uri_mobile: env::var("APPLE_REDIRECT_URI_MOBILE")
            .expect("APPLE_REDIRECT_URI_MOBILE must be set"),
    }
}

async fn make_oidc_client(
    apple_env: AppleEnv,
    callback_mode: CallbackMode,
    client_secret: Option<String>,
) -> CoreClient {
    let provider_metadata = CoreProviderMetadata::discover_async(
        IssuerUrl::new("https://appleid.apple.com".to_string()).unwrap(),
        async_http_client,
    )
    .await
    .unwrap();

    let redirect_uri = match callback_mode {
        CallbackMode::Server => apple_env.redirect_uri,
        CallbackMode::Mobile => apple_env.redirect_uri + "/redirect",
    };
    CoreClient::from_provider_metadata(
        provider_metadata,
        ClientId::new(apple_env.client_id),
        client_secret.map(|secret| ClientSecret::new(secret)),
    )
    .set_redirect_uri(RedirectUrl::new(redirect_uri).unwrap())
}

#[derive(Debug, Serialize, Deserialize)]
struct AppleJwtPayload {
    iss: String,
    iat: i64,
    exp: i64,
    aud: String,
    sub: String,
}

fn apple_client_secret(apple_env: AppleEnv) -> String {
    let issued_at = Utc::now();
    jsonwebtoken::encode(
        &Header {
            typ: None,
            alg: Algorithm::ES256,
            kid: Some(apple_env.key_id),
            ..Header::default()
        },
        &AppleJwtPayload {
            iss: apple_env.team_id,
            iat: issued_at.timestamp(),
            exp: issued_at
                .checked_add_signed(Duration::seconds(15777000))
                .unwrap()
                .timestamp(),
            aud: "https://appleid.apple.com".to_owned(),
            sub: apple_env.client_id,
        },
        &EncodingKey::from_ec_pem(apple_env.key_value.as_ref()).unwrap(),
    )
    .unwrap()
}

pub async fn auth_url(callback_mode: CallbackMode) -> String {
    let env = get_env();
    let client = make_oidc_client(env, callback_mode, None).await;
    let (auth_url, ..) = client
        .authorize_url(
            CoreAuthenticationFlow::AuthorizationCode,
            CsrfToken::new_random,
            Nonce::new_random,
        )
        .add_extra_param("response_mode", CoreResponseMode::FormPost.as_ref())
        .add_scope(Scope::new("name".to_string()))
        .add_scope(Scope::new("email".to_string()))
        .url();
    auth_url.to_string()
}

pub async fn login_from_code(
    db: &DatabaseConnection,
    callback_mode: CallbackMode,
    code: String,
    user: Option<AppleUser>,
) -> Result<(AuthTokenWithRefresh, ServiceToken<AuthToken>), ApiError> {
    let env = get_env();
    let client_secret = apple_client_secret(env.clone());
    let client = make_oidc_client(
        env.clone(),
        callback_mode.clone(),
        Some(client_secret.clone()),
    )
    .await;
    let token_response = client
        .exchange_code(AuthorizationCode::new(code))
        .add_extra_param("client_id", env.client_id)
        .add_extra_param("client_secret", client_secret)
        .request_async(async_http_client)
        .await
        .map_err(|err| AuthenticationError::OAuthUnknown(format!("{}", err)))?;

    struct IgnoreNonce {}
    impl NonceVerifier for IgnoreNonce {
        fn verify(self, _: Option<&Nonce>) -> Result<(), String> {
            Ok(())
        }
    }

    let claims = token_response
        .id_token()
        .unwrap()
        .claims(&client.id_token_verifier(), IgnoreNonce {})
        .unwrap()
        .to_owned();
    let login_dto = ServiceLoginDto {
        credentials: credentials_from_token_response(
            token_response,
            claims.subject().to_string(),
            None,
        ),
        account: user.map(|user| ServiceAccountDto {
            email: user.email,
            display_name: format!("{} {}", user.name.first_name, user.name.last_name),
            photo_url: None,
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
    let credentials = service::get_credentials_by_user_id(&db, user_id, Service::AppleMusic)
        .await
        .map_err(|_| AuthenticationError::OAuthRefreshTokenMissing)?;

    let env = get_env();
    let client_secret = apple_client_secret(env.clone());
    let client = make_oidc_client(
        env.clone(),
        CallbackMode::Server,
        Some(client_secret.clone()),
    )
    .await;
    let token_response = client
        .exchange_refresh_token(&RefreshToken::new(credentials.token.refresh_token.clone()))
        .add_extra_param("client_id", env.client_id)
        .add_extra_param("client_secret", client_secret)
        .request_async(async_http_client)
        .await
        .map_err(|err| AuthenticationError::OAuthUnknown(err.to_string()))?;

    let service_token = service::update_credentials(
        &db,
        &credentials_from_token_response(
            token_response,
            credentials.service_id,
            Some(credentials.token.refresh_token),
        ),
    )
    .await?;
    Ok(service_token.into_without_refresh())
}

fn credentials_from_token_response(
    response: CoreTokenResponse,
    service_id: String,
    refresh_token: Option<String>,
) -> ServiceCredentialsDto {
    ServiceCredentialsDto {
        service: Service::AppleMusic,
        service_id,
        access_token: response.access_token().secret().to_owned(),
        access_token_expires: response
            .expires_in()
            .and_then(|dur| Utc::now().checked_add_signed(Duration::from_std(dur).unwrap())),
        refresh_token: if let Some(token) = refresh_token {
            token
        } else {
            response.refresh_token().unwrap().secret().to_owned()
        },
        refresh_token_expires: None,
    }
}
