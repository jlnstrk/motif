use chrono::{DateTime, Utc};

use crate::domain::common::typedef::Service;

pub struct ServiceCredentialsDto {
    pub service: Service,
    pub service_id: String,
    pub access_token: String,
    pub access_token_expires: Option<DateTime<Utc>>,
    pub refresh_token: String,
    pub refresh_token_expires: Option<DateTime<Utc>>,
}

pub struct ServiceAccountDto {
    pub email: String,
    pub display_name: String,
    pub photo_url: Option<String>,
}

pub struct ServiceLoginDto {
    pub credentials: ServiceCredentialsDto,
    pub account: ServiceAccountDto,
}
