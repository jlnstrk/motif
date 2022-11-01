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
    pub account: Option<ServiceAccountDto>,
}
