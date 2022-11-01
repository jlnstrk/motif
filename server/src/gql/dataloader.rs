use crate::domain::{like, motif, profile};
use crate::rest::util::ApiError;
use async_graphql::dataloader::Loader;
use async_trait::async_trait;
use sea_orm::DatabaseConnection;
use std::collections::HashMap;
use uuid::Uuid;

pub struct MotifListenedLoader {
    pub db: DatabaseConnection,
    pub profile_id: Uuid,
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

pub struct MotifLikedLoader {
    pub db: DatabaseConnection,
    pub profile_id: Uuid,
}

#[async_trait]
impl Loader<i32> for MotifLikedLoader {
    type Value = bool;
    type Error = ApiError;

    async fn load(&self, keys: &[i32]) -> Result<HashMap<i32, Self::Value>, Self::Error> {
        let set = like::datasource::has_liked_motif_all(&self.db, self.profile_id, keys).await?;
        Ok(HashMap::from_iter(
            keys.into_iter().map(|id| (id.clone(), set.contains(id))),
        ))
    }
}

pub struct CommentLikedLoader {
    pub db: DatabaseConnection,
    pub profile_id: Uuid,
}

#[async_trait]
impl Loader<i32> for CommentLikedLoader {
    type Value = bool;
    type Error = ApiError;

    async fn load(&self, keys: &[i32]) -> Result<HashMap<i32, Self::Value>, Self::Error> {
        let set = like::datasource::has_liked_comment_all(&self.db, self.profile_id, keys).await?;
        Ok(HashMap::from_iter(
            keys.into_iter().map(|id| (id.clone(), set.contains(id))),
        ))
    }
}

pub struct ProfileFollowingLoader {
    pub db: DatabaseConnection,
    pub profile_id: Uuid,
}

#[async_trait]
impl Loader<Uuid> for ProfileFollowingLoader {
    type Value = bool;
    type Error = ApiError;

    async fn load(&self, keys: &[Uuid]) -> Result<HashMap<Uuid, Self::Value>, Self::Error> {
        let set = profile::datasource::is_following_all(&self.db, self.profile_id, keys).await?;
        Ok(HashMap::from_iter(
            keys.into_iter().map(|id| (id.clone(), set.contains(id))),
        ))
    }
}
