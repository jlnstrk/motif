#![allow(dead_code)]

use async_graphql::futures_util::Stream;
use async_graphql::*;
use async_graphql::{ComplexObject, Context, Object, Subscription};
use async_stream::stream;
use fred::prelude::RedisValue;
use uuid::Uuid;

use crate::domain::comment::typedef::Comment;
use crate::domain::motif::datasource;
use crate::domain::motif::pubsub::{
    topic_motif_created, topic_motif_deleted, topic_motif_listened,
};
use crate::domain::motif::typedef::{CreateMotif, Motif, ServiceId};
use crate::domain::profile::typedef::Profile;
use crate::domain::{comment, like, profile};
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ContextDependencies};
use crate::PubSubHandle;

#[ComplexObject]
impl Motif {
    async fn service_ids(&self, ctx: &Context<'_>) -> Result<Vec<ServiceId>> {
        datasource::get_service_ids_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn creator(&self, ctx: &Context<'_>) -> Result<Profile> {
        profile::datasource::get_by_id(ctx.require(), self.creator_id)
            .await
            .coerce_gql_err()
    }

    async fn listeners_count(&self, ctx: &Context<'_>) -> Result<i32> {
        datasource::get_listeners_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn listeners(&self, ctx: &Context<'_>) -> Result<Vec<Profile>> {
        datasource::get_listeners_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn comments_count(&self, ctx: &Context<'_>) -> Result<i32> {
        comment::datasource::get_motif_comments_count_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn comments(&self, ctx: &Context<'_>) -> Result<Vec<Comment>> {
        comment::datasource::get_motif_comments_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn likes_count(&self, ctx: &Context<'_>) -> Result<i32> {
        like::datasource::get_motif_likes_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn likes(&self, ctx: &Context<'_>) -> Result<Vec<Profile>> {
        like::datasource::get_motif_likes(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct MotifQuery;

#[Object]
impl MotifQuery {
    async fn motif_feed(&self, ctx: &Context<'_>) -> Result<Vec<Motif>> {
        datasource::get_feed_by_profile_id(ctx.require(), ctx.require::<AuthClaims>().id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct MotifMutation;

#[Object]
impl MotifMutation {
    async fn motif_create(&self, ctx: &Context<'_>, args: CreateMotif) -> Result<Motif> {
        let own_id = ctx.require::<AuthClaims>().id;
        let motif = datasource::create(ctx.require(), own_id.clone(), args).await?;
        let topic = topic_motif_created(own_id);
        ctx.require::<PubSubHandle<RedisValue>>()
            .publish(topic, RedisValue::Integer(motif.id.into()))
            .await;
        Ok(motif)
    }

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
    async fn motif_created<'a>(&'a self, ctx: &'a Context<'_>) -> impl Stream<Item = Motif> + 'a {
        let following_ids =
            profile::datasource::get_following_ids(ctx.require(), ctx.require::<AuthClaims>().id)
                .await
                .unwrap();
        let topics: Vec<String> = following_ids
            .into_iter()
            .map(|id| topic_motif_created(id))
            .collect();
        let mut subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(topics)
            .await;
        stream! {
            while let Some(value) = subscription.receive().await {
                if let RedisValue::Integer(motif_id) = value {
                    if let Some(motif) = datasource::get_by_id(ctx.require(), motif_id as i32).await.ok() {
                        yield motif;
                    }
                }
            }
        }
    }

    async fn motif_deleted<'a>(&'a self, ctx: &'a Context<'_>) -> impl Stream<Item = i32> + 'a {
        let following_ids =
            profile::datasource::get_following_ids(ctx.require(), ctx.require::<AuthClaims>().id)
                .await
                .unwrap();
        let topics: Vec<String> = following_ids
            .into_iter()
            .map(|id| topic_motif_deleted(id))
            .collect();
        let mut subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(topics)
            .await;
        stream! {
            while let Some(value) = subscription.receive().await {
                if let RedisValue::Integer(motif_id) = value {
                    yield motif_id as i32;
                }
            }
        }
    }

    async fn motif_listened<'a>(
        &'a self,
        ctx: &'a Context<'_>,
        motif_id: i32,
    ) -> impl Stream<Item = Profile> + 'a {
        let mut subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(vec![topic_motif_listened(motif_id)])
            .await;
        stream! {
            while let Some(value) = subscription.receive().await {
                if let RedisValue::String(listener_id) = value {
                    if let Some(profile) = profile::datasource::get_by_id(ctx.require(), Uuid::parse_str(&listener_id.to_string()).unwrap())
                        .await
                        .ok() {
                        yield profile;
                    }
                }
            }
        }
    }
}
