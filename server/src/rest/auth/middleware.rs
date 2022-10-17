use axum::http;
use axum::http::Request;
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};

use crate::domain::auth;
use crate::gql::util::AuthClaims;
use crate::rest::util::{ApiError, AuthenticationError};

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
    let header = req
        .headers()
        .get(http::header::AUTHORIZATION)
        .and_then(|header| header.to_str().ok());
    let token_string = header.and_then(|header| header.split(" ").last());
    let user = if let Some(token) = token_string {
        Some(auth::datasource::token::verify_access_jwt(token.to_string()).await?)
    } else {
        None
    };

    match user {
        None => {
            if fail_if_no_auth {
                Err(match (header, token_string, user) {
                    (None, ..) => ApiError::Authentication(AuthenticationError::TokenMissing),
                    (Some(_), None, ..) => {
                        ApiError::Authentication(AuthenticationError::TokenMalformed)
                    }
                    (Some(_), Some(_), ..) => {
                        ApiError::Authentication(AuthenticationError::UserNotFound)
                    }
                })
            } else {
                let mut req = req;
                req.extensions_mut().insert::<Option<AuthClaims>>(None);
                Ok(next.run(req).await)
            }
        }
        Some(user_id) => {
            let mut req = req;
            let extensions = req.extensions_mut();
            extensions.insert::<Option<AuthClaims>>(Some(AuthClaims { id: user_id }));
            extensions.insert::<AuthClaims>(AuthClaims { id: user_id });
            Ok(next.run(req).await)
        }
    }
}
