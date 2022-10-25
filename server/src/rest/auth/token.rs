use axum::http::StatusCode;
use axum::middleware::from_fn;
use axum::routing::post;
use axum::{Extension, Json, Router};
use sea_orm::DatabaseConnection;
use serde::Deserialize;

use crate::domain::auth;
use crate::domain::auth::typedef::AuthResponse;
use crate::gql::util::AuthClaims;
use crate::rest::auth::middleware::verify_jwt_middleware;
use crate::rest::util::ApiError;

pub fn token_router() -> Router {
    Router::new()
        .route(
            "/revoke",
            post(revoke).layer(from_fn(verify_jwt_middleware)),
        )
        .route("/refresh", post(refresh))
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct TokenPayload {
    token: String,
}

async fn revoke(
    Extension(db): Extension<DatabaseConnection>,
    Extension(claims): Extension<AuthClaims>,
    Json(payload): Json<TokenPayload>,
) -> Result<StatusCode, ApiError> {
    let _ = auth::datasource::token::revoke_refresh_jwt(&db, claims.id, payload.token).await?;
    Ok(StatusCode::OK)
}

async fn refresh(
    Extension(db): Extension<DatabaseConnection>,
    Json(payload): Json<TokenPayload>,
) -> Result<Json<AuthResponse>, ApiError> {
    let token = auth::datasource::token::refresh_jwt(&db, payload.token).await?;
    Ok(Json(token.into()))
}
