#![allow(dead_code)]

use async_graphql::dataloader::DataLoader;
use async_graphql::futures_util::Stream;
use async_graphql::*;
use fred::prelude::RedisValue;
use futures::stream::StreamExt;
use uuid::Uuid;

use crate::domain::collection::typedef::Collection;
use crate::domain::motif::typedef::Motif;
use crate::domain::profile::datasource;
use crate::domain::profile::pubsub::{topic_profile_followed, topic_profile_updated};
use crate::domain::profile::typedef::{Profile, ProfileUpdate};
use crate::domain::{collection, motif};
use crate::gql::auth::Authenticated;
use crate::gql::dataloader::ProfileFollowingLoader;
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ContextDependencies};
use crate::PubSubHandle;

#[ComplexObject]
impl Profile {
    async fn followers(&self, ctx: &Context<'_>) -> Result<Vec<Profile>> {
        datasource::get_followers(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn followers_count(&self, ctx: &Context<'_>) -> Result<i64> {
        datasource::get_followers_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn follows(&self, ctx: &Context<'_>) -> Result<bool> {
        let loader: &DataLoader<ProfileFollowingLoader> = ctx.require();
        loader
            .load_one(self.id)
            .await
            .map(|opt| opt.unwrap_or(false))
            .coerce_gql_err()
    }

    async fn following(&self, ctx: &Context<'_>) -> Result<Vec<Profile>> {
        datasource::get_following(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn following_count(&self, ctx: &Context<'_>) -> Result<i64> {
        datasource::get_following_count(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn motifs(&self, ctx: &Context<'_>) -> Result<Vec<Motif>> {
        motif::datasource::get_by_creator_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }

    async fn collections(&self, ctx: &Context<'_>) -> Result<Vec<Collection>> {
        collection::datasource::get_by_owner_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
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
    async fn profile_search(&self, ctx: &Context<'_>, query: String) -> Result<Vec<Profile>> {
        datasource::search(ctx.require(), query)
            .await
            .coerce_gql_err()
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
