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

use std::env;

use chrono::{Duration, FixedOffset, Utc};
use jsonwebtoken::Algorithm::HS256;
use jsonwebtoken::{DecodingKey, EncodingKey, Header, TokenData, Validation};
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, ConnectionTrait, DatabaseConnection, EntityTrait,
    ModelTrait, NotSet, QueryFilter, TransactionTrait,
};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use entity::refresh_tokens;
use entity::refresh_tokens::Entity as RefreshTokenEntity;

use crate::domain::auth::typedef::AuthTokenWithRefresh;
use crate::rest::util::{ApiError, AuthenticationError};

#[derive(Debug, Serialize, Deserialize)]
struct AppJwtPayload {
    sub: Uuid,
    exp: i64,
}

async fn verify_jwt(key_var: &str, token: String) -> Result<Uuid, ApiError> {
    let access_secret = env::var(key_var).expect(&format!("{} must be set", key_var));
    let decoding_key = DecodingKey::from_secret(access_secret.as_ref());
    let token_data: Option<TokenData<AppJwtPayload>> =
        jsonwebtoken::decode(&token, &decoding_key, &Validation::new(HS256)).ok();

    if let Some(ref data) = token_data {
        Ok(data.claims.sub)
    } else {
        Err(ApiError::Authentication(AuthenticationError::TokenInvalid))
    }
}

pub async fn verify_access_jwt(access_token: String) -> Result<Uuid, ApiError> {
    verify_jwt("ACCESS_TOKEN_SECRET", access_token).await
}

pub async fn verify_refresh_jwt(refresh_token: String) -> Result<Uuid, ApiError> {
    verify_jwt("REFRESH_TOKEN_SECRET", refresh_token).await
}

pub async fn issue_jwt_pair<C: ConnectionTrait>(
    db: &C,
    user_id: Uuid,
) -> Result<AuthTokenWithRefresh, ApiError> {
    let access_secret = env::var("ACCESS_TOKEN_SECRET").expect("ACCESS_JWT_SECRET must be set");
    let refresh_secret = env::var("REFRESH_TOKEN_SECRET").expect("REFRESH_JWT_SECRET must be set");

    let access_token_expires = Utc::now()
        .checked_add_signed(Duration::hours(1))
        .ok_or(AuthenticationError::TokenIssueFailed)?;
    let access_token = jsonwebtoken::encode(
        &Header::default(),
        &AppJwtPayload {
            sub: user_id,
            exp: access_token_expires.timestamp(),
        },
        &EncodingKey::from_secret(access_secret.as_ref()),
    )
    .map_err(|_| AuthenticationError::TokenIssueFailed)?;

    let refresh_token_expires = Utc::now()
        .checked_add_signed(Duration::hours(2 * 30 * 24))
        .ok_or(AuthenticationError::TokenIssueFailed)?;
    let refresh_token = jsonwebtoken::encode(
        &Header::default(),
        &AppJwtPayload {
            sub: user_id,
            exp: refresh_token_expires.timestamp(),
        },
        &EncodingKey::from_secret(refresh_secret.as_ref()),
    )
    .map_err(|_| AuthenticationError::TokenIssueFailed)?;

    let model = refresh_tokens::ActiveModel {
        id: NotSet,
        user_id: Set(user_id),
        value: Set(refresh_token.clone()),
        expires_at: Set(refresh_token_expires.with_timezone(&FixedOffset::east(0))),
    };
    let _ = model.insert(db).await?;

    Ok(AuthTokenWithRefresh {
        access_token,
        access_token_expires: Some(access_token_expires),
        refresh_token,
    })
}

pub async fn refresh_jwt(
    db: &DatabaseConnection,
    refresh_token: String,
) -> Result<AuthTokenWithRefresh, ApiError> {
    let user_id = verify_refresh_jwt(refresh_token.clone()).await?;

    let auth_token = db
        .transaction::<_, AuthTokenWithRefresh, ApiError>(|txn| {
            Box::pin(async move {
                revoke_refresh_jwt(txn, user_id, refresh_token).await?;
                issue_jwt_pair(txn, user_id).await
            })
        })
        .await?;

    Ok(auth_token)
}

pub async fn revoke_refresh_jwt<C: ConnectionTrait>(
    db: &C,
    user_id: Uuid,
    refresh_token: String,
) -> Result<bool, ApiError> {
    let persisted = RefreshTokenEntity::find()
        .filter(
            Condition::all()
                .add(refresh_tokens::Column::UserId.eq(user_id))
                .add(refresh_tokens::Column::Value.eq(refresh_token)),
        )
        .one(db)
        .await?;
    if let Some(model) = persisted {
        let delete = model.delete(db).await?;
        Ok(delete.rows_affected == 1)
    } else {
        Ok(false)
    }
}
