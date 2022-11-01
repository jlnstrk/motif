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
