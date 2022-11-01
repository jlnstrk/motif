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

#![allow(dead_code)]

use async_graphql::*;
use async_graphql::{ComplexObject, Context, Object};
use uuid::Uuid;

use crate::domain::collection::datasource;
use crate::domain::collection::typedef::{Collection, CreateCollection};
use crate::domain::motif::typedef::Motif;
use crate::gql::auth::Authenticated;
use crate::gql::util::{AuthClaims, CoerceGraphqlError, ContextDependencies};

#[ComplexObject]
impl Collection {
    async fn motifs(&self, ctx: &Context<'_>) -> Result<Vec<Motif>> {
        datasource::get_motifs_by_id(ctx.require(), self.id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct CollectionQuery;

#[Object]
impl CollectionQuery {
    #[graphql(guard = "Authenticated")]
    async fn collection_by_id(&self, ctx: &Context<'_>, collection_id: Uuid) -> Result<Collection> {
        datasource::get_by_id(ctx.require(), collection_id)
            .await
            .coerce_gql_err()
    }
}

#[derive(Default)]
pub struct CollectionMutation;

#[Object]
impl CollectionMutation {
    #[graphql(guard = "Authenticated")]
    async fn collection_create(
        &self,
        ctx: &Context<'_>,
        args: CreateCollection,
    ) -> Result<Collection> {
        let own_id = ctx.require::<AuthClaims>().id;
        let collection = datasource::create(ctx.require(), own_id, args)
            .await
            .coerce_gql_err()?;
        Ok(collection)
    }

    #[graphql(guard = "Authenticated")]
    async fn collection_delete_by_id(
        &self,
        ctx: &Context<'_>,
        collection_id: Uuid,
    ) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::delete_by_id(ctx.require(), own_id, collection_id)
            .await
            .coerce_gql_err()
    }

    #[graphql(guard = "Authenticated")]
    async fn collection_add_motif(
        &self,
        ctx: &Context<'_>,
        collection_id: Uuid,
        motif_id: i32,
    ) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::add_motif_by_id(ctx.require(), own_id.clone(), collection_id, motif_id)
            .await
            .coerce_gql_err()
    }

    #[graphql(guard = "Authenticated")]
    async fn collection_remove_motif(
        &self,
        ctx: &Context<'_>,
        collection_id: Uuid,
        motif_id: i32,
    ) -> Result<bool> {
        let own_id = ctx.require::<AuthClaims>().id;
        datasource::remove_motif_by_id(ctx.require(), own_id.clone(), collection_id, motif_id)
            .await
            .coerce_gql_err()
    }
}
