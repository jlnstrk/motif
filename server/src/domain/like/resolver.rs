#![allow(dead_code)]

use async_graphql::{Context, Object, Subscription};
use async_stream::stream;
use fred::prelude::RedisValue;
use futures::Stream;
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
        let mut subscription = ctx
            .require::<PubSubHandle<RedisValue>>()
            .subscribe(vec![topic_motif_liked(motif_id)])
            .await;
        stream! {
            while let Some(value) = subscription.receive().await {
                if let RedisValue::String(profile_id) = value {
                    if let Some(profile) = profile::datasource::get_by_id(ctx.require(), Uuid::parse_str(&profile_id.to_string()).unwrap()).await.ok() {
                        yield profile;
                    }
                }
            }
        }
    }
}
