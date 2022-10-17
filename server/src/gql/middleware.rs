use async_graphql::Schema;
use axum::http::Request;
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use fred::prelude::RedisValue;
use sea_orm::DatabaseConnection;

use crate::gql::schema::{Mutation, Query, Subscription};
use crate::gql::util::AuthClaims;
use crate::PubSubHandle;

pub async fn schema_middleware_auth<B>(
    req: Request<B>,
    next: Next<B>,
) -> Result<Response, impl IntoResponse> {
    let db: DatabaseConnection = req
        .extensions()
        .get::<DatabaseConnection>()
        .unwrap()
        .clone();
    let pubsub: PubSubHandle<RedisValue> = req
        .extensions()
        .get::<PubSubHandle<RedisValue>>()
        .unwrap()
        .clone();
    let claims: Option<AuthClaims> = req
        .extensions()
        .get::<Option<AuthClaims>>()
        .unwrap()
        .clone();
    let mut builder = Schema::build(
        Query::default(),
        Mutation::default(),
        Subscription::default(),
    )
    .data(db)
    .data(pubsub);
    if let Some(claims) = claims {
        builder = builder.data(claims);
    } else {
        builder = builder.introspection_only();
    }
    let schema = builder.finish();
    let mut req_mut = req;
    req_mut.extensions_mut().insert(schema);
    Ok::<_, ()>(next.run(req_mut).await)
}

pub async fn schema_middleware<B>(
    req: Request<B>,
    next: Next<B>,
) -> Result<Response, impl IntoResponse> {
    let db: DatabaseConnection = req
        .extensions()
        .get::<DatabaseConnection>()
        .unwrap()
        .clone();
    let pubsub: PubSubHandle<RedisValue> = req
        .extensions()
        .get::<PubSubHandle<RedisValue>>()
        .unwrap()
        .clone();
    let schema = Schema::build(
        Query::default(),
        Mutation::default(),
        Subscription::default(),
    )
    .data(db)
    .data(pubsub)
    .finish();
    let mut req_mut = req;
    req_mut.extensions_mut().insert(schema);
    Ok::<_, ()>(next.run(req_mut).await)
}
