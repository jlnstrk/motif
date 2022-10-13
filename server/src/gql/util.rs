use async_graphql::{Context, Error, ErrorExtensions, Result};
use sea_orm::DbErr;
use std::fmt::Display;
use std::sync::Arc;
use uuid::Uuid;

pub trait ContextDependencies {
    fn require<T: Sync + Send + 'static>(&self) -> &T;
    fn require_arc<T: Sync + Send + 'static>(&self) -> &T;
}

impl ContextDependencies for Context<'_> {
    fn require<T: Sync + Send + 'static>(&self) -> &T {
        self.data().unwrap()
    }
    fn require_arc<T: Sync + Send + 'static>(&self) -> &T {
        self.data::<Arc<T>>().unwrap()
    }
}

#[derive(Clone)]
pub struct AuthClaims {
    pub id: Uuid,
}

pub trait CoerceGraphqlError<T> {
    fn coerce_gql_err(self) -> Result<T>;
}

impl<T, E: Display> CoerceGraphqlError<T> for Result<T, E> {
    fn coerce_gql_err(self) -> Result<T> {
        self.map_err(|err| (&err).extend())
    }
}
