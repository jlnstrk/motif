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

use uuid::Uuid;

pub fn topic_profile_updated(profile_id: Uuid) -> String {
    format!("PROFILE_UPDATED.{}", profile_id.as_hyphenated())
}

pub fn topic_profile_followed(profile_id: Uuid) -> String {
    format!("PROFILE_FOLLOWED.{}", profile_id.as_hyphenated())
}
