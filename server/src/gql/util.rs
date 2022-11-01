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

use std::fmt::Display;
use std::sync::Arc;

use async_graphql::{Context, ErrorExtensions, Result};
use uuid::Uuid;

pub trait ContextDependencies {
    fn require<T: Sync + Send + 'static>(&self) -> &T;
    fn require_arc<T: Sync + Send + 'static>(&self) -> &T;
}

impl ContextDependencies for Context<'_> {
    fn require<T: Sync + Send + 'static>(&self) -> &T {
        self.data().unwrap()
    }
    fn require_arc<T: Sync + Send + 'static>(&self) -> &T {
        self.data::<Arc<T>>().unwrap()
    }
}

#[derive(Clone)]
pub struct AuthClaims {
    pub id: Uuid,
}

pub trait CoerceGraphqlError<T> {
    fn coerce_gql_err(self) -> Result<T>;
}

impl<T, E: Display> CoerceGraphqlError<T> for Result<T, E> {
    fn coerce_gql_err(self) -> Result<T> {
        self.map_err(|err| (&err).extend())
    }
}
