use crate::gql::schema::{Mutation, Query, Subscription};
use crate::gql::util::AuthClaims;
use crate::PubSubHandle;
use async_graphql::Schema;
use axum::http::Request;
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use fred::prelude::RedisValue;
use sea_orm::DatabaseConnection;

async fn schema_middleware<B>(
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
    let mut req_mut = req;
    req_mut.extensions_mut().insert(builder.finish());
    Ok::<_, ()>(next.run(req_mut).await)
}
