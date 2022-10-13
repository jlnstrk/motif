use std::borrow::Borrow;
use std::env;
use std::sync::Arc;

use crate::domain::auth::typedef::ErrorResponse;
use axum::body::HttpBody;
use jsonwebtoken::Algorithm::HS256;
use jsonwebtoken::{decode, DecodingKey, TokenData, Validation};
use sea_orm::DatabaseConnection;
use uuid::Uuid;

use crate::gql::util::AuthClaims;
use crate::rest::util::{ApiError, AuthError};

pub async fn verify_jwt(_db: &DatabaseConnection, token: String) -> Result<AuthClaims, AuthError> {
    let access_secret = env::var("ACCESS_TOKEN_SECRET").expect("ACCESS_TOKEN_SECRET must be set");
    let decoding_key = DecodingKey::from_base64_secret(&access_secret).unwrap();
    let token_data: Option<TokenData<Uuid>> =
        decode(&token, &decoding_key, &Validation::new(HS256)).ok();

    if let Some(ref data) = token_data {
        Ok(AuthClaims { id: data.claims })
    } else {
        Err(AuthError::TokenInvalid)
    }
}
