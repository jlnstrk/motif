use async_graphql::{InputObject, SimpleObject};
use chrono::{DateTime, Utc};
use uuid::Uuid;

use crate::domain::common::typedef::Service;

#[derive(SimpleObject)]
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

#[derive(InputObject)]
pub struct ServiceIdInput {
    pub service: Service,
    pub id: String,
}
