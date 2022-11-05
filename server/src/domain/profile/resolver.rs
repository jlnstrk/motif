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

#![allow(dead_code)]

use async_graphql::connection::{ConnectionNameType, EdgeNameType};
use async_graphql::dataloader::DataLoader;
use async_graphql::futures_util::Stream;
use async_graphql::*;
use fred::prelude::RedisValue;
use futures::stream::StreamExt;
use uuid::Uuid;

use crate::domain::collection::typedef::Collection;
use crate::domain::motif::dataloader::MotifsByProfileLoader;
use crate::domain::motif::typedef::Motif;
use crate::domain::profile::dataloader::ProfileFollowsLoader;
use crate::domain::profile::datasource;
use crate::domain::profile::pubsub::{topic_profile_followed, topic_profile_updated};
use crate::domain::profile::typedef::{Profile, ProfileUpdate};
use crate::domain::{collection, motif};
use crate::gql::auth::Authenticated;
use crate::gql::connection::{
    field_cursor_page, position_page, DateTimeCursor, FieldCursorConnection, PositionConnection,
};
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ConnectionParams, ContextDependencies};
use crate::PubSubHandle;

struct MotifsByCreatedAtConnection;

impl ConnectionNameType for MotifsByCreatedAtConnection {
    fn type_name<T: OutputType>() -> String {
        "MotifsByCreatedAtConnection".to_owned()
    }
}

struct MotifsByCreatedAtEdge;

impl EdgeNameType for MotifsByCreatedAtEdge {
    fn type_name<T: OutputType>() -> String {
        "MotifsByCreatedAtEdge".to_owned()
    }
}

struct ProfilesByPositionConnection;

impl ConnectionNameType for ProfilesByPositionConnection {
    fn type_name<T: OutputType>() -> String {
        "ProfilesByPositionConnection".to_owned()
    }
}

struct ProfilesByPositionEdge;

impl EdgeNameType for ProfilesByPositionEdge {
    fn type_name<T: OutputType>() -> String {
        "ProfilesByPositionEdge".to_owned()
    }
}

#[ComplexObject]
impl Profile {
    async fn followers(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| {
            datasource::get_followers(ctx.require(), self.id, limit, offset)
        })
        .await
    }

    async fn followers_count(&self, ctx: &Context<'_>) -> Result<i64> {
        datasource::get_followers_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn follows(&self, ctx: &Context<'_>) -> Result<bool> {
        let loader: &DataLoader<ProfileFollowsLoader> = ctx.require();
        loader
            .load_one(self.id)
            .await
            .map(|opt| opt.unwrap_or(false))
            .coerce_gql_err()
    }

    async fn following(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| {
            datasource::get_following(ctx.require(), self.id, limit, offset)
        })
        .await
    }

    async fn following_count(&self, ctx: &Context<'_>) -> Result<i64> {
        datasource::get_following_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn feed(&self, ctx: &Context<'_>) -> Result<Vec<Motif>> {
        let loader: &DataLoader<MotifsByProfileLoader> = ctx.require();
        loader
            .load_one(self.id)
            .await
            .map(|opt| opt.unwrap_or(Vec::new()))
            .coerce_gql_err()
    }

    async fn motifs(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<
        FieldCursorConnection<
            DateTimeCursor,
            Motif,
            MotifsByCreatedAtConnection,
            MotifsByCreatedAtEdge,
        >,
    > {
        field_cursor_page(
            page,
            |after, before, limit| {
                motif::datasource::get_by_creator_id(
                    ctx.require(),
                    self.id,
                    after.map(Into::into),
                    before.map(Into::into),
                    limit,
                )
            },
            |node| node.created_at.clone().into(),
        )
        .await
    }

    async fn collections(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Collection>> {
        position_page(page, |limit, offset| async move {
            collection::datasource::get_by_owner_id(ctx.require(), self.id, limit, offset).await
        })
        .await
    }
}

#[derive(Default)]
pub struct ProfileQuery;

#[Object]
impl ProfileQuery {
    #[graphql(guard = "Authenticated")]
    async fn profile_me(&self, ctx: &Context<'_>) -> Result<Profile> {
        datasource::get_by_id(ctx.require(), ctx.require::<AuthClaims>().id)
            .await
            .coerce_gql_err()
    }

    #[graphql(guard = "Authenticated")]
    async fn profile_by_id(&self, ctx: &Context<'_>, profile_id: Uuid) -> Result<Option<Profile>> {
        Ok(datasource::get_by_id(ctx.require(), profile_id).await.ok())
    }

    #[graphql(guard = "Authenticated")]
    async fn profile_by_username(
        &self,
        ctx: &Context<'_>,
        username: String,
    ) -> Result<Option<Profile>> {
        Ok(datasource::get_by_username(ctx.require(), username)
            .await
            .ok())
    }

    #[graphql(guard = "Authenticated")]
    async fn profile_search(
        &self,
        ctx: &Context<'_>,
        query: String,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| async move {
            datasource::search(ctx.require(), query, limit, offset).await
        })
        .await
    }

    async fn profile_is_username_available(
        &self,
        ctx: &Context<'_>,
        username: String,
    ) -> Result<bool> {
        datasource::is_username_available(ctx.require(), username)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct ProfileMutation;

#[Object]
impl ProfileMutation {
    #[graphql(guard = "Authenticated")]
    async fn profile_me_update(&self, ctx: &Context<'_>, update: ProfileUpdate) -> Result<Profile> {
        let profile_id = ctx.require::<AuthClaims>().id;
        let updated = datasource::update_by_id(ctx.require(), profile_id.clone(), update).await?;
        let topic = topic_profile_updated(profile_id);
        ctx.require::<PubSubHandle<RedisValue>>()
            .publish(topic, RedisValue::String("".into()))
            .await;
        Ok(updated)
    }

    #[graphql(guard = "Authenticated")]
    async fn profile_follow_by_id(&self, ctx: &Context<'_>, profile_id: Uuid) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        let new = datasource::follow(ctx.require(), own_id.clone(), profile_id)
            .await
            .coerce_gql_err()?;
        if new {
            let topic = topic_profile_followed(profile_id);
            ctx.require::<PubSubHandle<RedisValue>>()
                .publish(topic, RedisValue::String(own_id.to_string().into()))
                .await;
        }
        Ok(new)
    }

    #[graphql(guard = "Authenticated")]
    async fn profile_unfollow_by_id(&self, ctx: &Context<'_>, profile_id: Uuid) -> Result<bool> {
        datasource::unfollow(ctx.require(), ctx.require::<AuthClaims>().id, profile_id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct ProfileSubscription;

#[Subscription]
impl ProfileSubscription {
    #[graphql(guard = "Authenticated")]
    async fn profile_me<'a>(&self, ctx: &'a Context<'_>) -> impl Stream<Item = Profile> + 'a {
        let profile_id: Uuid = ctx.require::<AuthClaims>().id;
        let topic = topic_profile_updated(profile_id.clone());
        let subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(vec![topic])
            .await;
        subscription.stream().filter_map(move |_| async move {
            datasource::get_by_id(ctx.require(), profile_id.clone())
                .await
                .ok()
        })
    }

    #[graphql(guard = "Authenticated")]
    async fn profile_me_new_follower<'a>(
        &self,
        ctx: &'a Context<'_>,
    ) -> impl Stream<Item = Profile> + 'a {
        let profile_id: Uuid = ctx.require::<AuthClaims>().id;
        let topic = topic_profile_followed(profile_id.clone());
        let subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(vec![topic])
            .await;
        subscription.stream().filter_map(move |value| async move {
            if let RedisValue::String(follower_id) = value {
                datasource::get_by_id(
                    ctx.require(),
                    Uuid::parse_str(&follower_id.to_string()).unwrap(),
                )
                .await
                .ok()
            } else {
                None
            }
        })
    }
}
