use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde_json::json;
use std::fmt::{write, Debug, Display, Formatter};

#[derive(Debug)]
pub enum AuthError {
    TokenMissing,
    TokenMalformed,
    TokenInvalid,
    UserNotFound,
    OAuthInternal,
    OAuthBadCallback,
    OAuthInsufficientClaims,
    OAuthRefreshTokenMissing,
}

#[derive(Debug)]
pub enum ApiError {
    Auth(AuthError),
}

impl From<AuthError> for ApiError {
    fn from(inner: AuthError) -> Self {
        ApiError::Auth(inner)
    }
}

impl Display for AuthError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            AuthError::TokenMissing => write!(f, "Authorization token missing"),
            AuthError::TokenMalformed => write!(f, "Authorization token malformed"),
            AuthError::TokenInvalid => write!(f, "Authorization token invalid"),
            AuthError::UserNotFound => write!(f, "User not found"),
            AuthError::OAuthInternal => write!(f, "OAuth internal error"),
            AuthError::OAuthBadCallback => write!(f, "OAuth bad callback"),
            AuthError::OAuthInsufficientClaims => {
                write!(f, "OAuth insufficient claims")
            }
            AuthError::OAuthRefreshTokenMissing => write!(f, "OAuth refresh token missing"),
        }
    }
}

impl Display for ApiError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            ApiError::Auth(auth) => Display::fmt(auth, f),
        }
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let status = match self {
            ApiError::Auth(AuthError::TokenMissing) => StatusCode::UNAUTHORIZED,
            ApiError::Auth(AuthError::TokenMalformed) => StatusCode::UNAUTHORIZED,
            ApiError::Auth(AuthError::TokenInvalid) => StatusCode::UNAUTHORIZED,
            ApiError::Auth(AuthError::UserNotFound) => StatusCode::NOT_FOUND,
            ApiError::Auth(AuthError::OAuthInternal) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::Auth(AuthError::OAuthBadCallback) => StatusCode::BAD_REQUEST,
            ApiError::Auth(AuthError::OAuthInsufficientClaims) => StatusCode::PRECONDITION_FAILED,
            ApiError::Auth(AuthError::OAuthRefreshTokenMissing) => StatusCode::PRECONDITION_FAILED,
        };
        let message = format!("{}", self);
        (status, Json(json!({ "message": message }))).into_response()
    }
}
