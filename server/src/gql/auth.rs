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

use crate::gql::util::{AuthClaims, CoerceGraphqlError};
use crate::rest::util::{ApiError, AuthenticationError};
use async_graphql::*;

pub(crate) struct Authenticated;

#[async_trait::async_trait]
impl Guard for Authenticated {
    async fn check(&self, ctx: &Context<'_>) -> Result<()> {
        ctx.data_opt::<AuthClaims>()
            .ok_or(ApiError::Authentication(AuthenticationError::TokenInvalid))
            .coerce_gql_err()
            .map(|_| ())
    }
}
