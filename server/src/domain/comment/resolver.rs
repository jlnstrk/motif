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

use async_graphql::*;
use async_graphql::{ComplexObject, Context, Object};
use async_graphql::dataloader::DataLoader;
use fred::prelude::RedisValue;

use crate::domain::comment::datasource;
use crate::domain::comment::pubsub::{topic_comment_created, topic_comment_deleted};
use crate::domain::comment::typedef::{Comment, CreateComment};
use crate::domain::motif::typedef::Motif;
use crate::domain::profile::typedef::Profile;
use crate::domain::{like, motif, profile};
use crate::domain::comment::dataloader::CommentLikedLoader;
use crate::gql::auth::Authenticated;
use crate::gql::connection::{position_page, PositionConnection};
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ConnectionParams, ContextDependencies};
use crate::PubSubHandle;

#[ComplexObject]
impl Comment {
    async fn child_comments_count(&self, ctx: &Context<'_>) -> Result<i32> {
        datasource::get_child_comments_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn child_comments(&self, ctx: &Context<'_>, page: Option<ConnectionParams>) -> Result<PositionConnection<Comment>> {
        position_page(page, |limit, offset| {
            datasource::get_child_comments_by_id(ctx.require(), self.id, limit, offset)
        }).await
    }

    async fn author(&self, ctx: &Context<'_>) -> Result<Profile> {
        profile::datasource::get_by_id(ctx.require(), self.author_id)
            .await
            .coerce_gql_err()
    }

    async fn motif(&self, ctx: &Context<'_>) -> Result<Motif> {
        motif::datasource::get_by_id(ctx.require(), self.motif_id)
            .await
            .coerce_gql_err()
    }

    async fn parent_comment(&self, ctx: &Context<'_>) -> Result<Option<Comment>> {
        if let Some(parent_id) = self.parent_comment_id {
            Ok(Some(datasource::get_by_id(ctx.require(), parent_id).await?))
        } else {
            Ok(None)
        }
    }

    async fn liked(&self, ctx: &Context<'_>) -> Result<bool> {
        let loader: &DataLoader<CommentLikedLoader> = ctx.require();
        loader
            .load_one(self.id)
            .await
            .map(|opt| opt.unwrap_or(false))
            .coerce_gql_err()
    }

    async fn likes_count(&self, ctx: &Context<'_>) -> Result<i32> {
        like::datasource::get_comment_likes_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn likes(&self, ctx: &Context<'_>,page: Option<ConnectionParams>) -> Result<PositionConnection<Profile>> {
        position_page(page, |limit, offset| {
            like::datasource::get_comment_likes(ctx.require(), self.id, limit, offset)
        }).await
    }
}

#[derive(Default)]
pub struct CommentQuery;

#[Object]
impl CommentQuery {
    #[graphql(guard = "Authenticated")]
    async fn comment_by_id(&self, ctx: &Context<'_>, comment_id: i32) -> Result<Comment> {
        datasource::get_by_id(ctx.require(), comment_id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct CommentMutation;

#[Object]
impl CommentMutation {
    #[graphql(guard = "Authenticated")]
    async fn motif_comment_create(
        &self,
        ctx: &Context<'_>,
        motif_id: i32,
        args: CreateComment,
    ) -> Result<Comment> {
        let own_id = ctx.require::<AuthClaims>().id;
        let comment =
            datasource::create(ctx.require(), own_id.clone(), Some(motif_id), None, args).await?;
        let topic = topic_comment_created(motif_id);
        ctx.require::<PubSubHandle<RedisValue>>()
            .publish(topic, RedisValue::Integer(comment.id.into()))
            .await;
        Ok(comment)
    }

    #[graphql(guard = "Authenticated")]
    async fn motif_comment_create_sub(
        &self,
        ctx: &Context<'_>,
        parent_comment_id: i32,
        args: CreateComment,
    ) -> Result<Comment> {
        let own_id = ctx.require::<AuthClaims>().id;
        let comment = datasource::create(
            ctx.require(),
            own_id.clone(),
            None,
            Some(parent_comment_id),
            args,
        )
        .await?;
        Ok(comment)
    }

    #[graphql(guard = "Authenticated")]
    async fn comment_delete_by_id(&self, ctx: &Context<'_>, comment_id: i32) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        let comment = datasource::get_by_id(ctx.require(), comment_id).await?;
        let deleted = datasource::delete_by_id(ctx.require(), own_id.clone(), comment_id).await?;
        if deleted {
            let topic = topic_comment_deleted(comment.motif_id);
            ctx.require::<PubSubHandle<RedisValue>>()
                .publish(topic, RedisValue::Integer(comment.id.into()))
                .await;
        }
        Ok(deleted)
    }
}
