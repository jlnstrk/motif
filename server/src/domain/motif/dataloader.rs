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

use crate::domain::motif::datasource;
use crate::domain::motif::typedef::{Metadata, Motif};
use crate::domain::{feed, like};
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
        let set = datasource::has_listened_all(&self.db, self.profile_id, keys).await?;
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

pub struct MotifMetadataLoader {
    pub db: DatabaseConnection,
}

#[async_trait]
impl Loader<String> for MotifMetadataLoader {
    type Value = Metadata;
    type Error = ApiError;

    async fn load(&self, keys: &[String]) -> Result<HashMap<String, Self::Value>, Self::Error> {
        let map = datasource::get_metadata_all(&self.db, keys).await?;
        Ok(map)
    }
}

// Dedicated Loader to optimize the feed,
// which often involves loading motifs for each profile
pub struct MotifsByProfileLoader {
    pub db: DatabaseConnection,
}

#[async_trait]
impl Loader<Uuid> for MotifsByProfileLoader {
    type Value = Vec<Motif>;
    type Error = ApiError;

    async fn load(&self, keys: &[Uuid]) -> Result<HashMap<Uuid, Self::Value>, Self::Error> {
        let map = feed::datasource::get_motifs_by_profile_ids(&self.db, keys).await?;
        Ok(map)
    }
}
