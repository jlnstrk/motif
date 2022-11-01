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

use axum::extract::Query;
use axum::middleware::from_fn;
use axum::response::{IntoResponse, Redirect};
use axum::routing::{get, post};
use axum::{Extension, Json, Router};
use sea_orm::DatabaseConnection;
use serde::Deserialize;

use crate::domain::auth;
use crate::domain::auth::typedef::{AuthResponse, ServiceTokenResponse};
use crate::gql::util::AuthClaims;
use crate::rest::auth::middleware::verify_jwt_middleware;
use crate::rest::auth::CallbackModeQuery;
use crate::rest::util::{ApiError, AuthenticationError};

pub fn spotify_router() -> Router {
    Router::new()
        .route("/", get(auth))
        .route("/callback", get(callback))
        .route(
            "/refresh",
            post(refresh).layer(from_fn(verify_jwt_middleware)),
        )
}

async fn auth(Query(q): Query<CallbackModeQuery>) -> impl IntoResponse {
    let redirect_uri = auth::datasource::spotify::auth_url(q.callback_mode);
    Redirect::to(&redirect_uri)
}

#[derive(Deserialize, Clone)]
struct CodeQuery {
    code: Option<String>,
}

async fn callback(
    Extension(db): Extension<DatabaseConnection>,
    Query(params): Query<CodeQuery>,
    Query(q): Query<CallbackModeQuery>,
) -> Result<Json<AuthResponse>, ApiError> {
    let code = params
        .code
        .ok_or_else(|| AuthenticationError::OAuthBadCallback)?;

    let tokens =
        auth::datasource::spotify::login_from_auth_code(&db, q.callback_mode, code.clone()).await?;
    Ok(Json(tokens.into()))
}

async fn refresh(
    Extension(db): Extension<DatabaseConnection>,
    Extension(claims): Extension<AuthClaims>,
) -> Result<Json<ServiceTokenResponse>, ApiError> {
    let service_token = auth::datasource::spotify::refresh_from_user_id(&db, claims.id).await?;
    Ok(Json(service_token.into()))
}
