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

use apalis::prelude::Storage;
use apalis::redis::RedisStorage;
use async_graphql::dataloader::DataLoader;
use async_graphql::futures_util::Stream;
use async_graphql::*;
use async_graphql::{ComplexObject, Context, Object, Subscription};
use fred::prelude::RedisValue;
use futures_util::StreamExt;
use log::error;
use uuid::Uuid;

use crate::domain::comment::typedef::Comment;
use crate::domain::motif::dataloader::{
    MotifLikedLoader, MotifListenedLoader, MotifMetadataLoader,
};
use crate::domain::motif::datasource;
use crate::domain::motif::pubsub::{
    topic_motif_created, topic_motif_deleted, topic_motif_listened,
};
use crate::domain::motif::typedef::{CreateMotif, Metadata, Motif, ServiceId};
use crate::domain::profile::typedef::Profile;
use crate::domain::{comment, like, profile};
use crate::gql::auth::Authenticated;
use crate::gql::connection::{position_page, PositionConnection};
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ConnectionParams, ContextDependencies};
use crate::metadata::FetchMetadata;
use crate::PubSubHandle;

#[ComplexObject]
impl Motif {
    async fn metadata(&self, ctx: &Context<'_>) -> Result<Option<Metadata>> {
        let loader: &DataLoader<MotifMetadataLoader> = ctx.require();
        loader
            .load_one(self.isrc.clone())
            .await
            .map(|opt| opt)
            .coerce_gql_err()
    }

    async fn service_ids(&self, ctx: &Context<'_>) -> Result<Vec<ServiceId>> {
        datasource::get_service_ids_by_isrc(ctx.require(), self.isrc.clone())
            .await
            .coerce_gql_err()
    }

    async fn creator(&self, ctx: &Context<'_>) -> Result<Profile> {
        profile::datasource::get_by_id(ctx.require(), self.creator_id)
            .await
            .coerce_gql_err()
    }

    async fn listened(&self, ctx: &Context<'_>) -> Result<bool> {
        let loader: &DataLoader<MotifListenedLoader> = ctx.require();
        loader
            .load_one(self.id)
            .await
            .map(|opt| opt.unwrap_or(false))
            .coerce_gql_err()
    }

    async fn listeners_count(&self, ctx: &Context<'_>) -> Result<i32> {
        datasource::get_listeners_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn listeners(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| async move {
            datasource::get_listeners_by_id(ctx.require(), self.id, limit, offset).await
        })
        .await
    }

    async fn comments_count(&self, ctx: &Context<'_>) -> Result<i32> {
        comment::datasource::get_motif_comments_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn comments(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Comment>> {
        position_page(page, |limit, offset| {
            comment::datasource::get_motif_comments_by_id(ctx.require(), self.id, limit, offset)
        })
        .await
    }

    async fn liked(&self, ctx: &Context<'_>) -> Result<bool> {
        let loader: &DataLoader<MotifLikedLoader> = ctx.require();
        loader
            .load_one(self.id)
            .await
            .map(|opt| opt.unwrap_or(false))
            .coerce_gql_err()
    }

    async fn likes_count(&self, ctx: &Context<'_>) -> Result<i32> {
        like::datasource::get_motif_likes_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn likes(
        &self,
        ctx: &Context<'_>,
        page: Option<ConnectionParams>,
    ) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| {
            like::datasource::get_motif_likes(ctx.require(), self.id, limit, offset)
        })
        .await
    }
}

#[derive(Default)]
pub struct MotifQuery;

#[Object]
impl MotifQuery {
    #[graphql(guard = "Authenticated")]
    async fn motif_by_id(&self, ctx: &Context<'_>, motif_id: i32) -> Result<Motif> {
        datasource::get_by_id(ctx.require(), motif_id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct MotifMutation;

#[Object]
impl MotifMutation {
    #[graphql(guard = "Authenticated")]
    async fn motif_create(&self, ctx: &Context<'_>, args: CreateMotif) -> Result<Motif> {
        let own_id = ctx.require::<AuthClaims>().id;
        let motif = datasource::create(ctx.require(), own_id.clone(), args).await?;

        if let Err(err) = ctx
            .require::<RedisStorage<FetchMetadata>>()
            .clone()
            .push(FetchMetadata {
                isrc: motif.isrc.clone(),
            })
            .await
        {
            error!("motif_create: {}", err);
        }

        let topic = topic_motif_created(own_id);
        ctx.require::<PubSubHandle<RedisValue>>()
            .publish(topic, RedisValue::Integer(motif.id.into()))
            .await;

        Ok(motif)
    }

    #[graphql(guard = "Authenticated")]
    async fn motif_delete_by_id(&self, ctx: &Context<'_>, motif_id: i32) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        let deleted = datasource::delete_by_id(ctx.require(), own_id.clone(), motif_id).await?;
        if deleted {
            let topic = topic_motif_deleted(own_id);
            ctx.require::<PubSubHandle<RedisValue>>()
                .publish(topic, RedisValue::Integer(motif_id.into()))
                .await;
        }
        Ok(deleted)
    }

    #[graphql(guard = "Authenticated")]
    async fn motif_listen_by_id(&self, ctx: &Context<'_>, motif_id: i32) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        let is_new = datasource::listen_by_id(ctx.require(), own_id.clone(), motif_id).await?;
        if is_new {
            let topic = topic_motif_listened(motif_id);
            ctx.require::<PubSubHandle<RedisValue>>()
                .publish(topic, RedisValue::String(own_id.to_string().into()))
                .await;
        }
        Ok(is_new)
    }
}

#[derive(Default)]
pub struct MotifSubscription;

#[Subscription]
impl MotifSubscription {
    #[graphql(guard = "Authenticated")]
    async fn motif_created<'a>(&'a self, ctx: &'a Context<'_>) -> impl Stream<Item = Motif> + 'a {
        let following_ids =
            profile::datasource::get_following_ids(ctx.require(), ctx.require::<AuthClaims>().id)
                .await
                .unwrap();
        let topics: Vec<String> = following_ids
            .into_iter()
            .map(|id| topic_motif_created(id))
            .collect();
        let subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(topics)
            .await;
        subscription.stream().filter_map(move |value| async move {
            if let RedisValue::Integer(motif_id) = value {
                datasource::get_by_id(ctx.require(), motif_id as i32)
                    .await
                    .ok()
            } else {
                None
            }
        })
    }

    #[graphql(guard = "Authenticated")]
    async fn motif_deleted<'a>(&'a self, ctx: &'a Context<'_>) -> impl Stream<Item = i32> + 'a {
        let following_ids =
            profile::datasource::get_following_ids(ctx.require(), ctx.require::<AuthClaims>().id)
                .await
                .unwrap();
        let topics: Vec<String> = following_ids
            .into_iter()
            .map(|id| topic_motif_deleted(id))
            .collect();
        let subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(topics)
            .await;
        subscription.stream().filter_map(move |value| async move {
            if let RedisValue::Integer(motif_id) = value {
                Some(motif_id as i32)
            } else {
                None
            }
        })
    }

    #[graphql(guard = "Authenticated")]
    async fn motif_listened<'a>(
        &'a self,
        ctx: &'a Context<'_>,
        motif_id: i32,
    ) -> impl Stream<Item = Profile> + 'a {
        let subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(vec![topic_motif_listened(motif_id)])
            .await;
        subscription.stream().filter_map(move |value| async move {
            if let RedisValue::String(listener_id) = value {
                profile::datasource::get_by_id(
                    ctx.require(),
                    Uuid::parse_str(&listener_id.to_string()).unwrap(),
                )
                .await
                .ok()
            } else {
                None
            }
        })
    }
}
