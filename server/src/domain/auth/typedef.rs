use crate::domain::auth::typedef::ServiceToken::{AccessOnly, Full};
use crate::domain::common::typedef::Service;
use crate::Str;
use chrono::{DateTime, Utc};
use serde::Serialize;

#[derive(Serialize, Clone)]
pub struct ServiceTokenResponse {
    pub token: ServiceToken,
}

#[derive(Serialize, Clone)]
pub struct ErrorResponse {
    pub message: String,
}

#[derive(Serialize, Clone)]
pub struct AccessOnlyServiceToken {
    pub service: Service,
    pub access_token: String,
    pub access_token_expires: Option<DateTime<Utc>>,
}

#[derive(Serialize, Clone)]
pub struct FullServiceToken {
    pub service: Service,
    pub service_id: String,
    pub access_token: String,
    pub access_token_expires: Option<DateTime<Utc>>,
    pub refresh_token: String,
    pub refresh_token_expires: Option<DateTime<Utc>>,
}

#[derive(Serialize, Clone)]
#[serde(tag = "type")]
pub enum ServiceToken {
    AccessOnly(AccessOnlyServiceToken),
    Full(FullServiceToken),
}

impl From<ServiceToken> for ServiceTokenResponse {
    fn from(token: ServiceToken) -> Self {
        Self { token }
    }
}

impl From<FullServiceToken> for ServiceToken {
    fn from(token: FullServiceToken) -> Self {
        Full(token)
    }
}

impl From<AccessOnlyServiceToken> for ServiceToken {
    fn from(token: AccessOnlyServiceToken) -> Self {
        AccessOnly(token)
    }
}

impl From<FullServiceToken> for AccessOnlyServiceToken {
    fn from(full_token: FullServiceToken) -> Self {
        Self {
            service: full_token.service,
            access_token: full_token.access_token,
            access_token_expires: full_token.access_token_expires,
        }
    }
}

pub trait IntoAccessOnly {
    fn into_access_only(self) -> ServiceToken;
}

impl IntoAccessOnly for FullServiceToken {
    fn into_access_only(self) -> ServiceToken {
        AccessOnly(self.into())
    }
}
