/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
