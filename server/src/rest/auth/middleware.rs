use crate::gql::util::AuthClaims;
use crate::rest::auth::util::verify_jwt;
use crate::rest::util::{ApiError, AuthError};
use axum::http;
use axum::http::Request;
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use sea_orm::DatabaseConnection;

pub async fn verify_jwt_middleware_no_fail<B>(
    req: Request<B>,
    next: Next<B>,
) -> Result<Response, impl IntoResponse> {
    verify_jwt_middleware_explicit(req, next, false).await
}

pub async fn verify_jwt_middleware<B>(
    req: Request<B>,
    next: Next<B>,
) -> Result<Response, impl IntoResponse> {
    verify_jwt_middleware_explicit(req, next, true).await
}

pub async fn verify_jwt_middleware_explicit<B>(
    req: Request<B>,
    next: Next<B>,
    fail_if_no_auth: bool,
) -> Result<Response, impl IntoResponse> {
    let db = req
        .extensions()
        .get::<DatabaseConnection>()
        .expect("No database");
    let header = req
        .headers()
        .get(http::header::AUTHORIZATION)
        .and_then(|header| header.to_str().ok());
    let token_string = header.and_then(|header| header.matches(" ").last());
    let user = if let Some(token) = token_string {
        Some(verify_jwt(db, token.to_string()).await?)
    } else {
        None
    };

    match user {
        None => {
            if fail_if_no_auth {
                Err(match (header, token_string, user) {
                    (None, ..) => ApiError::Auth(AuthError::TokenMissing),
                    (Some(_), None, ..) => ApiError::Auth(AuthError::TokenMalformed),
                    (Some(_), Some(_), ..) => ApiError::Auth(AuthError::UserNotFound),
                })
            } else {
                let mut req = req;
                req.extensions_mut().insert::<Option<AuthClaims>>(None);
                Ok(next.run(req).await)
            }
        }
        Some(claims) => {
            let mut req = req;
            req.extensions_mut().insert(Some(claims));
            Ok(next.run(req).await)
        }
    }
}
