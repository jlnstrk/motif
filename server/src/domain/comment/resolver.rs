use std::error::Error;

use async_graphql::*;
use async_graphql::{ComplexObject, Context, Object};
use fred::prelude::RedisValue;

use crate::domain::comment::datasource;
use crate::domain::comment::pubsub::{topic_comment_created, topic_comment_deleted};
use crate::domain::comment::typedef::{Comment, CreateComment};
use crate::domain::motif::typedef::Motif;
use crate::domain::profile::typedef::Profile;
use crate::domain::{motif, profile};
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ContextDependencies};
use crate::PubSubHandle;
use sea_orm::DbErr;

#[ComplexObject]
impl Comment {
    async fn child_comments_count(&self, ctx: &Context<'_>) -> Result<i32> {
        datasource::get_child_comments_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn child_comments(&self, ctx: &Context<'_>) -> Result<Vec<Comment>> {
        datasource::get_child_comments_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
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
}

//#[ComplexObject]
impl Motif {
    async fn comments_count(&self, ctx: &Context<'_>) -> Result<i32> {
        datasource::get_motif_comments_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn comments(&self, ctx: &Context<'_>) -> Result<Vec<Comment>> {
        datasource::get_motif_comments_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct CommentQuery;

#[Object]
impl CommentQuery {
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
    async fn comment_create_to_motif(
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

    async fn comment_create_to_comment(
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
