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

use chrono::{DateTime, Utc};
use serde::Serialize;

use crate::domain::common::typedef::Service;

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct AuthResponse {
    pub app_token: AuthTokenWithRefresh,
    pub service_tokens: Vec<ServiceToken<AuthToken>>,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct AuthToken {
    pub access_token: String,
    pub access_token_expires: Option<DateTime<Utc>>,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct AuthTokenWithRefresh {
    pub access_token: String,
    pub access_token_expires: Option<DateTime<Utc>>,
    pub refresh_token: String,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ServiceTokenResponse {
    pub service_token: ServiceToken<AuthToken>,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ErrorResponse {
    pub message: String,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ServiceToken<T> {
    pub service: Service,
    pub service_id: String,
    pub token: T,
}

impl AuthTokenWithRefresh {
    fn into_without_refresh(self) -> AuthToken {
        AuthToken {
            access_token: self.access_token,
            access_token_expires: self.access_token_expires,
        }
    }
}

impl ServiceToken<AuthTokenWithRefresh> {
    pub(crate) fn into_without_refresh(self) -> ServiceToken<AuthToken> {
        ServiceToken {
            service: self.service,
            service_id: self.service_id,
            token: self.token.into_without_refresh(),
        }
    }
}

impl From<ServiceToken<AuthToken>> for ServiceTokenResponse {
    fn from(token: ServiceToken<AuthToken>) -> Self {
        Self {
            service_token: token,
        }
    }
}

impl From<(AuthTokenWithRefresh, ServiceToken<AuthToken>)> for AuthResponse {
    fn from((auth_token, service_token): (AuthTokenWithRefresh, ServiceToken<AuthToken>)) -> Self {
        Self {
            app_token: auth_token,
            service_tokens: vec![service_token],
        }
    }
}

impl From<AuthTokenWithRefresh> for AuthResponse {
    fn from(auth_token: AuthTokenWithRefresh) -> Self {
        Self {
            app_token: auth_token,
            service_tokens: vec![],
        }
    }
}
