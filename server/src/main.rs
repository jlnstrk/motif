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

extern crate core;

use apalis::cron::{CronWorker, Schedule};
use apalis::layers::DefaultRetryPolicy;
use apalis::prelude::{job_fn, Monitor, WorkerBuilder, WorkerFactoryFn};
use apalis::redis::RedisStorage;
use std::env;
use std::error::Error;
use std::net::SocketAddr;
use std::str::FromStr;
use std::time::Duration;

use async_graphql::futures_util::{pin_mut, StreamExt};
use axum::{Extension, Router, Server};
use axum_server::tls_rustls::RustlsConfig;
use dotenvy::dotenv;
use env_logger::Target;
use fred::types::RedisValue;
use futures_util::future;
use log::{info, LevelFilter};
use sea_orm::{DatabaseConnection, SqlxPostgresConnector};
use sqlx::postgres::{PgConnectOptions, PgPoolOptions};
use sqlx::ConnectOptions;
use tokio::sync::Mutex;
use tower::ServiceBuilder;

use crate::gql::routing::graphql_router;
use crate::metadata::{fetch_metadata, schedule_fetch_metadata, FetchMetadata};
use crate::pubsub::prelude::{PubSub, PubSubHandle};
use crate::pubsub::redis::RedisPubSubEngine;
use crate::rest::rest_router;

mod db;
mod domain;
mod gql;
mod metadata;
mod pubsub;
mod rest;

async fn make_db_connection() -> DatabaseConnection {
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");

    let mut options = database_url.parse::<PgConnectOptions>().unwrap();
    options.log_statements(LevelFilter::Debug);

    let pg_pool = PgPoolOptions::new()
        .max_connections(5)
        .acquire_timeout(Duration::from_secs(8))
        .idle_timeout(Duration::from_secs(8))
        .connect_with(options)
        .await
        .unwrap();

    sqlx::migrate!().run(&pg_pool).await.unwrap();

    SqlxPostgresConnector::from_sqlx_postgres_pool(pg_pool)
}

async fn make_redis_connection() -> Box<Mutex<RedisPubSubEngine>> {
    let redis_url = env::var("REDIS_URL").expect("REDIS_URL must be set");
    RedisPubSubEngine::new(redis_url).await
}

async fn make_metadata_job_storage() -> RedisStorage<FetchMetadata> {
    let redis_url = env::var("REDIS_URL").expect("REDIS_URL must be set");
    RedisStorage::connect(redis_url).await.unwrap()
}

async fn set_up_app(
    db: &DatabaseConnection,
    redis_pubsub: &PubSub<RedisValue>,
    metadata_job_storage: &RedisStorage<FetchMetadata>,
) -> Router {
    Router::new()
        .nest("/", rest_router())
        .nest("/graphql", graphql_router())
        .layer(Extension(db.clone()))
        .layer(Extension(PubSubHandle::from(redis_pubsub).await))
        .layer(Extension(metadata_job_storage.clone()))
}

async fn set_up_job_monitor(
    db: &DatabaseConnection,
    metadata_job_storage: &RedisStorage<FetchMetadata>,
) -> Result<(), Box<dyn Error>> {
    Monitor::new()
        .register(
            WorkerBuilder::new(metadata_job_storage.clone())
                .layer(apalis::layers::RetryLayer::new(DefaultRetryPolicy))
                .layer(apalis::layers::Extension(db.clone()))
                .build_fn(fetch_metadata),
        )
        .register(CronWorker::new(
            Schedule::from_str("0 * * * * * *").unwrap(),
            ServiceBuilder::new()
                .layer(apalis::layers::Extension(db.clone()))
                .layer(apalis::layers::Extension(metadata_job_storage.clone()))
                .service(job_fn(schedule_fetch_metadata)),
        ))
        .run()
        .await
        .map_err(|err| err.into())
}

async fn start_server(app: Router) -> Result<(), Box<dyn Error>> {
    let https = env::var("SCHEME").unwrap_or("https".to_owned()) == "https";
    if https && cfg!(debug_assertions) {
        info!("GraphiQL IDE: http://localhost:8080/graphql");
        let config = RustlsConfig::from_pem_file("./cert.pem", "./key.pem")
            .await
            .unwrap();
        let socket_addr = SocketAddr::from(([127, 0, 0, 1], 8080));
        axum_server::bind_rustls(socket_addr, config)
            .serve(app.into_make_service())
            .await
            .map(|_| ())
            .map_err(|err| err.into())
    } else {
        let socket_addr = SocketAddr::from(([0, 0, 0, 0, 0, 0, 0, 0], 8080));
        Server::bind(&socket_addr)
            .serve(app.into_make_service())
            .await
            .map(|_| ())
            .map_err(|err| err.into())
    }
}

#[tokio::main]
async fn main() {
    env::set_var("RUST_LOG", "debug");
    env_logger::builder().target(Target::Stdout).init();
    dotenv().ok();

    let db_connection: DatabaseConnection = make_db_connection().await;
    let redis: PubSub<RedisValue> = PubSub::connect(make_redis_connection().await).await;
    let metadata_job_storage: RedisStorage<FetchMetadata> = make_metadata_job_storage().await;

    let app: Router = set_up_app(&db_connection, &redis, &metadata_job_storage).await;

    let monitor = set_up_job_monitor(&db_connection, &metadata_job_storage);
    let server = start_server(app);

    future::try_join(monitor, server).await.unwrap();
}
