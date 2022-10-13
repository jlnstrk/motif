use crate::rest::auth::util::verify_jwt_middleware;
use axum::http::StatusCode;
use axum::middleware::from_fn;
use axum::response::IntoResponse;
use axum::routing::{get, post, Route};
use axum::Router;

pub fn apple_router() -> Router {
    Router::new()
        .route("/", get(auth))
        .route("/callback", post(callback))
        .route(
            "/refresh",
            post(refresh).layer(from_fn(verify_jwt_middleware)),
        )
}

async fn auth() -> impl IntoResponse {
    StatusCode::OK
}

async fn callback() -> impl IntoResponse {
    StatusCode::OK
}

async fn refresh() -> impl IntoResponse {
    StatusCode::OK
}
