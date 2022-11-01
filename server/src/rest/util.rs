/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use std::error::Error;
use std::fmt::{Debug, Display, Formatter};

use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use sea_orm::{DbErr, TransactionError};
use serde_json::json;

#[derive(Debug, Clone)]
pub enum AuthenticationError {
    TokenMissing,
    TokenInvalid,
    TokenIssueFailed,
    TokenRevoked,
    UserNotFound,
    UserCancelled,
    OAuthUnknown(String),
    OAuthBadCallback,
    OAuthInsufficientClaims,
    OAuthRefreshTokenMissing,
}

#[derive(Debug, Clone)]
pub enum GeneralError {
    Database(DbErr),
    Internal,
}

#[derive(Debug, Clone)]
pub enum DataError {
    NotFound(String),
}

#[derive(Debug, Clone)]
pub enum ApiError {
    Authentication(AuthenticationError),
    Authorization(String),
    General(GeneralError),
    Data(DataError),
}

impl From<AuthenticationError> for ApiError {
    fn from(inner: AuthenticationError) -> Self {
        ApiError::Authentication(inner)
    }
}

impl From<GeneralError> for ApiError {
    fn from(inner: GeneralError) -> Self {
        ApiError::General(inner)
    }
}

impl From<DataError> for ApiError {
    fn from(inner: DataError) -> Self {
        ApiError::Data(inner)
    }
}

impl Display for AuthenticationError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            AuthenticationError::TokenMissing => write!(f, "Authorization token missing"),
            AuthenticationError::TokenInvalid => write!(f, "Authorization token invalid"),
            AuthenticationError::TokenIssueFailed => write!(f, "Failed to issue auth token"),
            AuthenticationError::TokenRevoked => write!(f, "Refresh token has been revoked"),
            AuthenticationError::UserNotFound => write!(f, "User not found"),
            AuthenticationError::UserCancelled => write!(f, "User cancelled"),
            AuthenticationError::OAuthUnknown(message) => {
                write!(f, "OAuth unknown error: {}", message)
            }
            AuthenticationError::OAuthBadCallback => write!(f, "OAuth bad callback"),
            AuthenticationError::OAuthInsufficientClaims => {
                write!(f, "OAuth insufficient claims")
            }
            AuthenticationError::OAuthRefreshTokenMissing => {
                write!(f, "OAuth refresh token missing")
            }
        }
    }
}

impl Display for GeneralError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            GeneralError::Database(source) => write!(f, "Database error: {}", source),
            GeneralError::Internal => write!(f, "Internal error"),
        }
    }
}

impl Display for DataError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            DataError::NotFound(message) => write!(f, "Data error: {}", message),
        }
    }
}

impl Display for ApiError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            ApiError::Authentication(auth) => Display::fmt(auth, f),
            ApiError::Authorization(message) => write!(f, "Missing authorization: {}", message),
            ApiError::Data(data) => Display::fmt(data, f),
            ApiError::General(general) => Display::fmt(general, f),
        }
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let status = match self {
            ApiError::Authentication(AuthenticationError::TokenMissing) => StatusCode::UNAUTHORIZED,

            ApiError::Authentication(AuthenticationError::TokenInvalid) => StatusCode::UNAUTHORIZED,
            ApiError::Authentication(AuthenticationError::TokenIssueFailed) => {
                StatusCode::INTERNAL_SERVER_ERROR
            }
            ApiError::Authentication(AuthenticationError::TokenRevoked) => StatusCode::UNAUTHORIZED,
            ApiError::Authentication(AuthenticationError::UserNotFound) => StatusCode::NOT_FOUND,
            ApiError::Authentication(AuthenticationError::UserCancelled) => StatusCode::ACCEPTED,
            ApiError::Authentication(AuthenticationError::OAuthUnknown(_)) => {
                StatusCode::INTERNAL_SERVER_ERROR
            }
            ApiError::Authentication(AuthenticationError::OAuthBadCallback) => {
                StatusCode::BAD_REQUEST
            }
            ApiError::Authentication(AuthenticationError::OAuthInsufficientClaims) => {
                StatusCode::PRECONDITION_FAILED
            }
            ApiError::Authentication(AuthenticationError::OAuthRefreshTokenMissing) => {
                StatusCode::PRECONDITION_FAILED
            }
            ApiError::Authorization(_) => StatusCode::UNAUTHORIZED,
            ApiError::Data(DataError::NotFound(_)) => StatusCode::NOT_FOUND,
            ApiError::General(GeneralError::Database(_)) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::General(GeneralError::Internal) => StatusCode::INTERNAL_SERVER_ERROR,
        };
        let message = format!("{}", self);
        (status, Json(json!({ "message": message }))).into_response()
    }
}

impl Error for ApiError {}

pub type ApiResult<R> = Result<R, ApiError>;

impl From<DbErr> for ApiError {
    fn from(err: DbErr) -> Self {
        ApiError::General(GeneralError::Database(err))
    }
}

impl<Inner: Error> From<TransactionError<Inner>> for ApiError {
    fn from(txn: TransactionError<Inner>) -> Self {
        ApiError::General(GeneralError::Database(match txn {
            TransactionError::Connection(db_err) => db_err,
            TransactionError::Transaction(conn) => DbErr::Conn(conn.to_string()),
        }))
    }
}
