use async_graphql::{InputObject, SimpleObject};
use chrono::{DateTime, Utc};
use uuid::Uuid;

#[derive(SimpleObject)]
#[graphql(complex)]
pub struct Comment {
    pub id: i32,
    pub text: String,
    pub offset: Option<i32>,
    pub created_at: DateTime<Utc>,
    pub author_id: Uuid,
    #[graphql(skip)]
    pub motif_id: i32,
    #[graphql(skip)]
    pub parent_comment_id: Option<i32>,
}

#[derive(InputObject)]
pub struct CreateComment {
    pub text: String,
    pub offset: Option<i32>,
}
