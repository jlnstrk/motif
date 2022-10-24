use crate::gql::util::{AuthClaims, CoerceGraphqlError};
use crate::rest::util::{ApiError, AuthenticationError};
use async_graphql::*;

pub(crate) struct Authenticated;

#[async_trait::async_trait]
impl Guard for Authenticated {
    async fn check(&self, ctx: &Context<'_>) -> Result<()> {
        ctx.data_opt::<AuthClaims>()
            .ok_or(ApiError::Authentication(AuthenticationError::TokenInvalid))
            .coerce_gql_err()
            .map(|_| ())
    }
}
