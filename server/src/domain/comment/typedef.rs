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
