use std::error::Error;

use async_graphql::{ComplexObject, Context, Object, Subscription};
use async_stream::stream;
use fred::prelude::RedisValue;
use futures::Stream;
use sea_orm::DbErr;

use crate::domain::comment::typedef::Comment;
use crate::domain::like::datasource;
use crate::domain::like::pubsub::topic_motif_liked;
use crate::domain::motif::typedef::Motif;
use crate::domain::profile;
use crate::domain::profile::typedef::Profile;
use crate::gql::util::{AuthClaims, ContextDependencies};
use crate::PubSubHandle;
use futures_util::stream::Map;
use uuid::Uuid;

//#[ComplexObject]
impl Comment {
    async fn likes_count(&self, ctx: &Context<'_>) -> Result<i32, DbErr> {
        datasource::get_comment_likes_count(ctx.require(), self.id).await
    }

    async fn likes(&self, ctx: &Context<'_>) -> Result<Vec<Profile>, DbErr> {
        datasource::get_comment_likes(ctx.require(), self.id).await
    }
}

//#[ComplexObject]
impl Motif {
    async fn likes_count(&self, ctx: &Context<'_>) -> Result<i32, DbErr> {
        datasource::get_motif_likes_count(ctx.require(), self.id).await
    }

    async fn likes(&self, ctx: &Context<'_>) -> Result<Vec<Profile>, DbErr> {
        datasource::get_motif_likes(ctx.require(), self.id).await
    }
}

#[derive(Default)]
pub struct LikeMutation;

#[Object]
impl LikeMutation {
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

    async fn comment_like_by_id(&self, ctx: &Context<'_>, comment_id: i32) -> Result<bool, DbErr> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::like_comment(ctx.require(), own_id, comment_id).await
    }

    async fn motif_unlike_by_id(&self, ctx: &Context<'_>, motif_id: i32) -> Result<bool, DbErr> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::unlike_motif(ctx.require(), own_id, motif_id).await
    }

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
