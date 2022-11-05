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

use sea_orm::QuerySelect;

pub trait OptLimitOffset: Sized {
    fn opt_limit_offset(self, limit: Option<u64>, offset: Option<u64>) -> Self;
}

impl<QueryStatement, T: QuerySelect<QueryStatement = QueryStatement>> OptLimitOffset for T {
    fn opt_limit_offset(mut self, limit: Option<u64>, offset: Option<u64>) -> Self {
        if let Some(limit) = limit {
            self = self.limit(limit);
        }
        if let Some(offset) = offset {
            self = self.offset(offset);
        }
        self
    }
}
