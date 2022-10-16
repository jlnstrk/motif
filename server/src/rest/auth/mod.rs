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
