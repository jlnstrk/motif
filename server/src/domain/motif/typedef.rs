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

use async_graphql::{InputObject, SimpleObject};
use chrono::{DateTime, Utc};
use uuid::Uuid;

use crate::domain::common::typedef::Service;

#[derive(Clone, SimpleObject)]
#[graphql(complex)]
pub struct Motif {
    pub id: i32,
    pub isrc: String,
    pub offset: i32,
    pub created_at: DateTime<Utc>,
    pub creator_id: Uuid,
}

#[derive(InputObject)]
pub struct CreateMotif {
    pub isrc: String,
    pub service_ids: Vec<ServiceIdInput>,
    pub offset: i32,
}

#[derive(SimpleObject)]
pub struct ServiceId {
    pub service: Service,
    pub id: String,
}

#[derive(Clone, SimpleObject)]
pub struct Metadata {
    pub name: String,
    pub artist: String,
    pub cover_art_url: Option<String>,
}

#[derive(InputObject)]
pub struct ServiceIdInput {
    pub service: Service,
    pub id: String,
}
