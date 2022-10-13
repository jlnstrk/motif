use crate::rest::auth::auth_router;
use crate::rest::auth::util::verify_jwt_middleware;
use axum::http::StatusCode;
use axum::middleware::from_fn;
use axum::response::IntoResponse;
use axum::routing::{get, post};
use axum::Router;

pub fn token_router() -> Router {
    Router::new()
        .route(
            "/revoke",
            post(revoke).layer(from_fn(verify_jwt_middleware)),
        )
        .route(
            "/refresh",
            post(refresh).layer(from_fn(verify_jwt_middleware)),
        )
}

async fn revoke() -> impl IntoResponse {
    StatusCode::OK
}

async fn refresh() -> impl IntoResponse {
    StatusCode::OK
}
