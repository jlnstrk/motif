use async_graphql::{InputObject, SimpleObject};
use uuid::Uuid;

#[derive(SimpleObject)]
#[graphql(complex)]
pub struct Profile {
    pub id: Uuid,
    pub display_name: String,
    pub username: String,
    pub photo_url: Option<String>,
    pub biography: Option<String>,
}

#[derive(InputObject)]
pub struct ProfileUpdate {
    pub display_name: Option<String>,
    pub username: Option<String>,
    pub photo_url: Option<String>,
    pub biography: Option<String>,
}
