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

use crate::domain::feed::datasource;
use crate::domain::motif::typedef::Motif;
use crate::domain::profile::typedef::Profile;
use crate::gql::auth::Authenticated;
use crate::gql::connection::{
    field_cursor_page, position_page, DateTimeCursor, FieldCursorConnection, PositionConnection,
};
use crate::gql::util::{AuthClaims, ConnectionParams, ContextDependencies};
use async_graphql::Result;
use async_graphql::{Context, Object};

#[derive(Default)]
pub struct FeedQuery;

#[Object]
impl FeedQuery {
    #[graphql(guard = "Authenticated")]
    async fn feed_motifs(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<FieldCursorConnection<DateTimeCursor, Motif>> {
        field_cursor_page(
            page,
            |after, before, limit| {
                datasource::get_motifs_by_profile_id(
                    ctx.require(),
                    ctx.require::<AuthClaims>().id,
                    after.map(Into::into),
                    before.map(Into::into),
                    limit,
                )
            },
            |node| node.created_at.clone().into(),
        )
        .await
    }

    #[graphql(guard = "Authenticated")]
    async fn feed_profiles(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| {
            datasource::get_profiles_by_profile_id(
                ctx.require(),
                ctx.require::<AuthClaims>().id,
                limit,
                offset,
            )
        })
        .await
    }
}
