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

use std::collections::HashMap;
use async_graphql::dataloader::Loader;
use async_trait::async_trait;
use sea_orm::DatabaseConnection;
use uuid::Uuid;
use crate::domain::like;
use crate::rest::util::ApiError;

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
