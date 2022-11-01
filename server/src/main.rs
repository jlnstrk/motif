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

use std::env;
use std::net::SocketAddr;
use std::time::Duration;

use async_graphql::futures_util::{pin_mut, StreamExt};
use axum::{Extension, Router, Server};
use axum_server::tls_rustls::RustlsConfig;
use dotenvy::dotenv;
use env_logger::Target;
use log::{info, LevelFilter};
use sea_orm::{DatabaseConnection, SqlxPostgresConnector};
use sqlx::postgres::{PgConnectOptions, PgPoolOptions};
use sqlx::ConnectOptions;
use tokio::sync::Mutex;

use crate::gql::routing::graphql_router;
use crate::pubsub::prelude::{PubSub, PubSubHandle};
use crate::pubsub::redis::RedisPubSubEngine;
use crate::rest::rest_router;

mod domain;
mod gql;
mod pubsub;
mod rest;

async fn make_db_connection() -> DatabaseConnection {
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");

    let mut options = database_url.parse::<PgConnectOptions>().unwrap();
    options.log_statements(LevelFilter::Trace);

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

#[tokio::main]
async fn main() {
    env::set_var("RUST_LOG", "info");
    env_logger::builder().target(Target::Stdout).init();
    dotenv().ok();

    let redis = PubSub::connect(make_redis_connection().await).await;
    let db_connection: DatabaseConnection = make_db_connection().await;

    let app = Router::new()
        .nest("/", rest_router())
        .nest("/graphql", graphql_router())
        .layer(Extension(db_connection))
        .layer(Extension(PubSubHandle::from(&redis).await));

    if cfg!(debug_assertions) {
        info!("GraphiQL IDE: http://localhost:8080/graphql");
        let config = RustlsConfig::from_pem_file("./cert.pem", "./key.pem")
            .await
            .unwrap();
        let socket_addr = SocketAddr::from(([127, 0, 0, 1], 8080));
        axum_server::bind_rustls(socket_addr, config)
            .serve(app.into_make_service())
            .await
            .unwrap()
    } else {
        let socket_addr = SocketAddr::from(([0, 0, 0, 0, 0, 0, 0, 0], 8080));
        Server::bind(&socket_addr)
            .serve(app.into_make_service())
            .await
            .unwrap()
    }
}
