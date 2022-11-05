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

use async_graphql::{Context, Object, Subscription};
use fred::prelude::RedisValue;
use futures::Stream;
use futures_util::StreamExt;
use sea_orm::DbErr;
use uuid::Uuid;

use crate::domain::like::datasource;
use crate::domain::like::pubsub::topic_motif_liked;
use crate::domain::profile;
use crate::domain::profile::typedef::Profile;
use crate::gql::auth::Authenticated;
use crate::gql::util::{AuthClaims, ContextDependencies};
use crate::PubSubHandle;

#[derive(Default)]
pub struct LikeMutation;

#[Object]
impl LikeMutation {
    #[graphql(guard = "Authenticated")]
    async fn motif_like_by_id(&self, ctx: &Context<'_>, motif_id: i32) -> Result<bool, DbErr> {
        let own_id = ctx.require::<AuthClaims>().id;
        let new = datasource::like_motif(ctx.require(), own_id, motif_id).await?;
        if new {
            let topic = topic_motif_liked(motif_id);
            ctx.require::<PubSubHandle<RedisValue>>()
                .publish(topic, RedisValue::String(own_id.to_string().into()))
                .await;
        }
        Ok(new)
    }

    #[graphql(guard = "Authenticated")]
    async fn comment_like_by_id(&self, ctx: &Context<'_>, comment_id: i32) -> Result<bool, DbErr> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::like_comment(ctx.require(), own_id, comment_id).await
    }

    #[graphql(guard = "Authenticated")]
    async fn motif_unlike_by_id(&self, ctx: &Context<'_>, motif_id: i32) -> Result<bool, DbErr> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::unlike_motif(ctx.require(), own_id, motif_id).await
    }

    #[graphql(guard = "Authenticated")]
    async fn comment_unlike_by_id(
        &self,
        ctx: &Context<'_>,
        comment_id: i32,
    ) -> Result<bool, DbErr> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::unlike_comment(ctx.require(), own_id, comment_id).await
    }
}

#[derive(Default)]
pub struct LikeSubscription;

#[Subscription]
impl LikeSubscription {
    #[graphql(guard = "Authenticated")]
    async fn motif_liked<'a>(
        &'a self,
        ctx: &'a Context<'_>,
        motif_id: i32,
    ) -> impl Stream<Item = Profile> + 'a {
        let subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(vec![topic_motif_liked(motif_id)])
            .await;
        subscription.stream().filter_map(move |value| async move {
            if let RedisValue::String(profile_id) = value {
                profile::datasource::get_by_id(
                    ctx.require(),
                    Uuid::parse_str(&profile_id.to_string()).unwrap(),
                )
                .await
                .ok()
            } else {
                None
            }
        })
    }
}
