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

use crate::gql::dataloader::{
    CommentLikedLoader, MotifLikedLoader, MotifListenedLoader, MotifMetadataLoader,
    ProfileFollowsLoader,
};
use apalis::redis::RedisStorage;
use async_graphql::dataloader::DataLoader;
use async_graphql::{Schema, SchemaBuilder};
use axum::http::Request;
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use fred::prelude::RedisValue;
use sea_orm::DatabaseConnection;

use crate::gql::schema::{Mutation, Query, Subscription};
use crate::gql::util::AuthClaims;
use crate::metadata::FetchMetadata;
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
    let storage: RedisStorage<FetchMetadata> = req
        .extensions()
        .get::<RedisStorage<FetchMetadata>>()
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
    .data(db.clone())
    .data(pubsub)
    .data(storage);
    if let Some(claims) = claims {
        builder = builder.data(claims.clone());
        builder = add_data_loaders(builder, db, claims);
    } else {
        builder = builder.introspection_only()
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

fn add_data_loaders(
    builder: SchemaBuilder<Query, Mutation, Subscription>,
    db: DatabaseConnection,
    claims: AuthClaims,
) -> SchemaBuilder<Query, Mutation, Subscription> {
    builder
        .data(DataLoader::new(
            MotifListenedLoader {
                db: db.clone(),
                profile_id: claims.id,
            },
            tokio::spawn,
        ))
        .data(DataLoader::new(
            MotifLikedLoader {
                db: db.clone(),
                profile_id: claims.id,
            },
            tokio::spawn,
        ))
        .data(DataLoader::new(
            CommentLikedLoader {
                db: db.clone(),
                profile_id: claims.id,
            },
            tokio::spawn,
        ))
        .data(DataLoader::new(
            ProfileFollowsLoader {
                db: db.clone(),
                profile_id: claims.id,
            },
            tokio::spawn,
        ))
        .data(DataLoader::new(
            MotifMetadataLoader { db: db.clone() },
            tokio::spawn,
        ))
}
