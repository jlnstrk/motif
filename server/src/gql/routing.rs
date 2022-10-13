use std::sync::Arc;

use async_graphql::http::{GraphiQLSource, ALL_WEBSOCKET_PROTOCOLS};
use async_graphql::{Data, Error};
use async_graphql_axum::{GraphQLProtocol, GraphQLRequest, GraphQLResponse, GraphQLWebSocket};
use axum::extract::WebSocketUpgrade;
use axum::middleware::from_fn;
use axum::response::{Html, IntoResponse};
use axum::routing::{get, post};
use axum::{Extension, Router};
use sea_orm::DatabaseConnection;
use serde::Deserialize;

use crate::gql::schema::AppSchema;
use crate::gql::{AppSchema, Mutation, Query, Subscription};
use crate::rest::auth::middleware::verify_jwt_middleware_no_fail;
use crate::rest::auth::util::{verify_jwt, verify_jwt_middleware_nf};
use crate::rest::util::AuthError;

pub async fn on_connection_init(
    db: Arc<DatabaseConnection>,
    value: serde_json::Value,
) -> Result<Data, Error> {
    #[derive(Deserialize)]
    struct ConnectPayload {
        token: String,
    }
    if let Ok(payload) = serde_json::from_value::<ConnectPayload>(value) {
        let mut data = Data::default();
        let claims = verify_jwt(&db, payload.token).await?;
        data.insert(claims);
        Ok(data)
    } else {
        Err(AuthError::TokenMissing.into())
    }
}

async fn graphql_ws_handler(
    Extension(schema): Extension<AppSchema>,
    Extension(db): Extension<Arc<DatabaseConnection>>,
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

async fn graphiql() -> impl IntoResponse {
    Html(
        GraphiQLSource::build()
            .endpoint("http://localhost:8080/graphql")
            .subscription_endpoint("ws://localhost:8080/graphql/ws")
            .finish(),
    )
}

pub fn graphql_router() -> Router {
    Router::new()
        .route("/", post(graphql_handler))
        .layer(from_fn(schema_middleware))
        .layer(from_fn(verify_jwt_middleware_no_fail))
        .route("/ws", get(graphql_ws_handler))
        .route("/", get(graphiql))
}
