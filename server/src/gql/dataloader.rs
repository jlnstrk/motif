use crate::domain::motif;
use crate::rest::util::ApiError;
use async_graphql::dataloader::Loader;
use async_trait::async_trait;
use sea_orm::DatabaseConnection;
use std::collections::HashMap;
use uuid::Uuid;

pub struct MotifListenedLoader {
    pub(crate) db: DatabaseConnection,
    pub(crate) profile_id: Uuid,
}

#[async_trait]
impl Loader<i32> for MotifListenedLoader {
    type Value = bool;
    type Error = ApiError;

    async fn load(&self, keys: &[i32]) -> Result<HashMap<i32, Self::Value>, Self::Error> {
        let set = motif::datasource::has_listened_all(&self.db, self.profile_id, keys).await?;
        Ok(HashMap::from_iter(
            keys.into_iter().map(|id| (id.clone(), set.contains(id))),
        ))
    }
}
