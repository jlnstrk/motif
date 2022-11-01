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

use entity::sea_orm_active_enums::Service as DbService;

use crate::domain::common::typedef::Service;

impl From<DbService> for Service {
    fn from(db_type: DbService) -> Self {
        match db_type {
            DbService::AppleMusic => Service::AppleMusic,
            DbService::Spotify => Service::Spotify,
        }
    }
}

impl From<Service> for DbService {
    fn from(db_type: Service) -> Self {
        match db_type {
            Service::AppleMusic => DbService::AppleMusic,
            Service::Spotify => DbService::Spotify,
        }
    }
}
