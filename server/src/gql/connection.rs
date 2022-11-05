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

use std::cmp::max;
use std::fmt::Display;
use std::future::Future;
use std::ops::Deref;

use async_graphql::connection::{
    Connection, ConnectionNameType, CursorType, DefaultConnectionName, DefaultEdgeName, Edge,
    EdgeNameType, EmptyFields,
};
use async_graphql::{connection, OutputType};
use chrono::{DateTime, Utc};

use crate::gql::util::{CoerceGraphqlError, ConnectionParams};

pub type PositionConnection<
    Node,
    ConnectionName = DefaultConnectionName,
    EdgeName = DefaultEdgeName,
> = Connection<i32, Node, EmptyFields, EmptyFields, ConnectionName, EdgeName>;

pub async fn position_page<Node, Query, Result, Error, ConnectionName, EdgeName>(
    params: Option<ConnectionParams>,
    query: Query,
) -> async_graphql::Result<Connection<i32, Node, EmptyFields, EmptyFields, ConnectionName, EdgeName>>
where
    Node: OutputType,
    Query: FnOnce(Option<u64>, Option<u64>) -> Result,
    Result: Future<Output = std::result::Result<Vec<Node>, Error>>,
    Error: Into<Box<dyn std::error::Error>> + Display,
    ConnectionName: ConnectionNameType,
    EdgeName: EdgeNameType,
{
    let params = params.unwrap_or(ConnectionParams::default());
    connection::query(
        params.after,
        params.before,
        params.first,
        params.last,
        |after: Option<i32>, before: Option<i32>, first, last| async move {
            let mut limit: Option<u64> = None;
            let mut offset: Option<u64> = None;

            if let Some(first) = first {
                limit = Some(first as u64);
            }
            if let Some(after) = after {
                offset = Some((after as u64) + 1);
            }

            if let Some(last) = last {
                let before = before.ok_or(async_graphql::Error::new(
                    "Cannot query 'last' without 'before'",
                ))?;
                offset = Some(max(0, (before as usize) - last) as u64);
                limit = Some(last as u64);
            }

            let nodes: Vec<Node> = query(limit, offset).await.coerce_gql_err()?;

            let mut connection = Connection::new(
                (first.is_some() && offset != Some(0)) || (last.is_some() && offset.is_some()),
                limit
                    .map(|limit| limit > nodes.len() as u64)
                    .unwrap_or(false),
            );

            connection
                .edges
                .extend(nodes.into_iter().enumerate().map(|(index, node)| {
                    Edge::with_additional_fields(index as i32, node, EmptyFields)
                }));
            Ok::<_, async_graphql::Error>(connection)
        },
    )
    .await
}

pub type FieldCursorConnection<
    Cursor,
    Node,
    ConnectionName = DefaultConnectionName,
    EdgeName = DefaultEdgeName,
> = Connection<Cursor, Node, EmptyFields, EmptyFields, ConnectionName, EdgeName>;

pub async fn field_cursor_page<
    Cursor,
    Node,
    Query,
    Result,
    Edge,
    EdgeCursor,
    ConnectionName,
    EdgeName,
>(
    params: Option<ConnectionParams>,
    query: Query,
    edge_cursor: EdgeCursor,
) -> async_graphql::Result<
    Connection<Cursor, Node, EmptyFields, EmptyFields, ConnectionName, EdgeName>,
>
where
    Cursor: Clone + CursorType + Send + Sync,
    <Cursor as CursorType>::Error: Display + Send + Sync + 'static,
    Node: OutputType,
    Query: FnOnce(Option<Cursor>, Option<Cursor>, Option<u64>) -> Result,
    Result: Future<Output = std::result::Result<Vec<Node>, Edge>>,
    Edge: Into<Box<dyn std::error::Error>> + Display,
    EdgeCursor: Fn(&Node) -> Cursor,
    ConnectionName: ConnectionNameType,
    EdgeName: EdgeNameType,
{
    let params = params.unwrap_or(ConnectionParams::default());
    connection::query(
        params.after,
        params.before,
        params.first,
        params.last,
        |after, before, first, last| async move {
            if before.is_some() && last.is_some() {
                panic!("Cannot query 'before' with 'last'");
            }

            let nodes: Vec<Node> = query(after, before, first.map(|first| first as u64))
                .await
                .coerce_gql_err()?;

            let mut connection = Connection::new(
                first.is_some(),
                first.map(|first| first <= nodes.len()).unwrap_or(false),
            );

            connection.edges.extend(nodes.into_iter().map(|node| {
                connection::Edge::with_additional_fields(edge_cursor(&node), node, EmptyFields)
            }));
            Ok::<_, async_graphql::Error>(connection)
        },
    )
    .await
}

#[derive(Debug, Copy, Clone)]
pub struct DateTimeCursor(DateTime<Utc>);

impl Deref for DateTimeCursor {
    type Target = DateTime<Utc>;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl From<DateTimeCursor> for DateTime<Utc> {
    fn from(cursor: DateTimeCursor) -> Self {
        cursor.0
    }
}

impl From<DateTime<Utc>> for DateTimeCursor {
    fn from(dt: DateTime<Utc>) -> Self {
        Self(dt)
    }
}

impl CursorType for DateTimeCursor {
    type Error = chrono::format::ParseError;

    fn decode_cursor(s: &str) -> std::result::Result<Self, Self::Error> {
        DateTime::parse_from_rfc3339(s).map(|dt| DateTimeCursor(dt.with_timezone(&Utc)))
    }

    fn encode_cursor(&self) -> String {
        self.0.to_rfc3339()
    }
}
