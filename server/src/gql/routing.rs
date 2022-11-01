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

use async_graphql::http::{GraphiQLSource, ALL_WEBSOCKET_PROTOCOLS};
use async_graphql::{Data, Error};
use async_graphql_axum::{GraphQLProtocol, GraphQLRequest, GraphQLResponse, GraphQLWebSocket};
use axum::extract::{Host, WebSocketUpgrade};
use axum::handler::Handler;
use axum::middleware::from_fn;
use axum::response::{Html, IntoResponse};
use axum::routing::{get, post};
use axum::{Extension, Router};
use sea_orm::DatabaseConnection;
use serde::Deserialize;
use std::env;

use crate::domain::auth::datasource::token::verify_access_jwt;
use crate::gql::middleware::{schema_middleware, schema_middleware_auth};
use crate::gql::schema::AppSchema;
use crate::gql::util::AuthClaims;
use crate::rest::auth::middleware::verify_jwt_middleware_no_fail;
use crate::rest::util::AuthenticationError;

pub async fn on_connection_init(
    _db: DatabaseConnection,
    value: serde_json::Value,
) -> Result<Data, Error> {
    #[derive(Deserialize)]
    struct ConnectPayload {
        token: String,
    }
    if let Ok(payload) = serde_json::from_value::<ConnectPayload>(value) {
        let mut data = Data::default();
        let user_id = verify_access_jwt(payload.token).await?;
        data.insert(AuthClaims { id: user_id });
        Ok(data)
    } else {
        Err(AuthenticationError::TokenMissing.into())
    }
}

async fn graphql_ws_handler(
    Extension(schema): Extension<AppSchema>,
    Extension(db): Extension<DatabaseConnection>,
    protocol: GraphQLProtocol,
    websocket: WebSocketUpgrade,
) -> impl IntoResponse {
    websocket
        .protocols(ALL_WEBSOCKET_PROTOCOLS)
        .on_upgrade(move |stream| {
            GraphQLWebSocket::new(stream, schema.clone(), protocol)
                .on_connection_init(|value| on_connection_init(db, value))
                .serve()
        })
}

async fn graphql_handler(schema: Extension<AppSchema>, req: GraphQLRequest) -> GraphQLResponse {
    schema.execute(req.into_inner()).await.into()
}

async fn graphiql(Host(host): Host) -> impl IntoResponse {
    let port = env::var("PORT").ok();
    let host = if let Some(port) = port {
        format!("{}:{}", host, port)
    } else {
        host
    };
    Html(
        GraphiQLSource::build()
            .endpoint(&format!("https://{}/graphql", host))
            .subscription_endpoint(&format!("ws://{}/graphql/ws", host))
            .finish(),
    )
}

pub fn graphql_router() -> Router {
    Router::new()
        .route("/", post(graphql_handler))
        .layer(from_fn(schema_middleware_auth))
        .layer(from_fn(verify_jwt_middleware_no_fail))
        .route(
            "/ws",
            get(graphql_ws_handler.layer(from_fn(schema_middleware))),
        )
        .route("/", get(graphiql))
}
