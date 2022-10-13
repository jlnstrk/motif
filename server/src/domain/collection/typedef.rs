use async_graphql::{InputObject, SimpleObject};
use chrono::{DateTime, Utc};
use uuid::Uuid;

#[derive(SimpleObject)]
#[graphql(complex)]
pub struct Collection {
    pub id: Uuid,
    pub title: String,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub owner_id: Uuid,
}

#[derive(InputObject)]
pub struct CreateCollection {
    pub title: String,
    pub description: Option<String>,
}
