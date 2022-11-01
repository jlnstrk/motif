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
use axum::{Extension, Form, Json, Router};
use sea_orm::DatabaseConnection;
use serde::{Deserialize, Serialize};
use serde_with::json;
use serde_with::serde_as;

use crate::domain::auth;
use crate::domain::auth::typedef::{AuthResponse, ServiceTokenResponse};
use crate::gql::util::AuthClaims;
use crate::rest::auth::middleware::verify_jwt_middleware;
use crate::rest::auth::{CallbackMode, CallbackModeQuery};
use crate::rest::util::{ApiError, AuthenticationError};

pub fn apple_router() -> Router {
    Router::new()
        .route("/", get(auth))
        .route("/callback", post(callback))
        .route("/callback/redirect", post(callback_redir))
        .route("/callback/mobile", get(callback_mobile))
        .route(
            "/refresh",
            post(refresh).layer(from_fn(verify_jwt_middleware)),
        )
}

async fn auth(Query(q): Query<CallbackModeQuery>) -> impl IntoResponse {
    let redirect_uri = auth::datasource::apple::auth_url(q.callback_mode).await;
    Redirect::to(&redirect_uri)
}
#[derive(Deserialize, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AppleName {
    pub first_name: String,
    pub last_name: String,
}

#[derive(Deserialize, Debug, Serialize)]
pub struct AppleUser {
    pub name: AppleName,
    pub email: String,
}

#[derive(Deserialize, Debug, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum AppleError {
    UserCancelledAuthorize,
}

#[serde_as]
#[derive(Deserialize, Debug, Serialize)]
pub struct AppleForm {
    state: Option<String>,
    code: Option<String>,
    id_token: Option<String>,
    #[serde(default)]
    #[serde_as(as = "json::JsonString")]
    user: Option<AppleUser>,
    error: Option<AppleError>,
}

async fn callback(
    Extension(db): Extension<DatabaseConnection>,
    Form(form): Form<AppleForm>,
    Query(q): Query<CallbackModeQuery>,
) -> Result<Json<AuthResponse>, ApiError> {
    if let Some(error) = form.error {
        match error {
            AppleError::UserCancelledAuthorize => {
                Err(ApiError::Authentication(AuthenticationError::UserCancelled))
            }
        }
    } else {
        let auth_response = auth::datasource::apple::login_from_code(
            &db,
            q.callback_mode,
            form.code.unwrap(),
            form.user,
        )
        .await?;
        Ok(Json(auth_response.into()))
    }
}

/// Callback endpoint to be invoked from Apple's sign-in page.
/// Redirects to a mobile scheme while converting the form contents to query params.
/// Needed to allow the mobile client to capture the callback's token response
/// Flow: Apple -> callback_redir -> Mobile -> callback_mobile
async fn callback_redir(Form(form): Form<AppleForm>) -> impl IntoResponse {
    let apple_env = auth::datasource::apple::get_env();
    let redirect_uri = reqwest::Client::new()
        .get(apple_env.redirect_uri_mobile)
        .query(&form)
        .build()
        .unwrap()
        .url()
        .to_string();
    Redirect::to(&redirect_uri)
}

/// Callback endpoint to be invoked from mobile clients
async fn callback_mobile(
    db_ext: Extension<DatabaseConnection>,
    Query(form): Query<AppleForm>,
) -> impl IntoResponse {
    callback(
        db_ext,
        Form(form),
        Query(CallbackModeQuery {
            callback_mode: CallbackMode::Mobile,
        }),
    )
    .await
}

async fn refresh(
    Extension(db): Extension<DatabaseConnection>,
    Extension(claims): Extension<AuthClaims>,
) -> Result<Json<ServiceTokenResponse>, ApiError> {
    let service_token = auth::datasource::apple::refresh_from_user_id(&db, claims.id).await?;
    Ok(Json(service_token.into()))
}
