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

use axum::Router;
use serde::Deserialize;

use crate::rest::auth::apple::apple_router;
use crate::rest::auth::spotify::spotify_router;
use crate::rest::auth::token::token_router;

pub mod apple;
pub mod middleware;
pub mod spotify;
pub mod token;

pub fn auth_router() -> Router {
    Router::new()
        .nest("/apple", apple_router())
        .nest("/spotify", spotify_router())
        .nest("/token", token_router())
}

#[derive(Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub enum CallbackMode {
    Server,
    Mobile,
}

impl Default for CallbackMode {
    fn default() -> Self {
        CallbackMode::Server
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct CallbackModeQuery {
    #[serde(default)]
    callback_mode: CallbackMode,
}
