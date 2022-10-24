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
    fail_if_missing: bool,
) -> Result<Response, ApiError> {
    let header = req
        .headers()
        .get(http::header::AUTHORIZATION)
        .and_then(|header| header.to_str().ok());
    if fail_if_missing && header.is_none() {
        Err(ApiError::Authentication(AuthenticationError::TokenMissing))?;
    }
    let token_string = header.and_then(|header| header.split(" ").last());
    let user = if let Some(token) = token_string {
        Some(auth::datasource::token::verify_access_jwt(token.to_string()).await?)
    } else {
        None
    };

    match user {
        None => {
            let mut req = req;
            req.extensions_mut().insert::<Option<AuthClaims>>(None);
            Ok(next.run(req).await)
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
