use crate::rest::auth::apple::apple_router;
use crate::rest::auth::spotify::spotify_router;
use crate::rest::auth::token::token_router;
use axum::Router;

pub mod apple;
pub mod middleware;
pub mod spotify;
pub mod token;
pub mod util;

pub fn auth_router() -> Router {
    Router::new()
        .nest("/apple", apple_router())
        .nest("/spotify", spotify_router())
        .nest("/token", token_router())
}
