use axum::Router;

use crate::rest::auth::auth_router;

pub mod auth;
pub mod util;

pub fn rest_router() -> Router {
    Router::new().nest("/auth", auth_router())
}
