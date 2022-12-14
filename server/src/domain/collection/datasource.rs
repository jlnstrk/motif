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

use chrono::{FixedOffset, Utc};
use itertools::Itertools;
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, EntityTrait, ModelTrait, NotSet,
    QueryFilter,
};
use uuid::Uuid;

use crate::db::util::OptLimitOffset;
use entity::collection_motifs::{Entity as CollectionMotifEntity, Model as CollectionMotifModel};
use entity::collections::{Entity as CollectionEntity, Model as CollectionModel};
use entity::motifs::{Entity as MotifEntity, Model as MotifModel};
use entity::{collection_motifs, collections};

use crate::domain::collection::typedef::{Collection, CreateCollection};
use crate::domain::motif::typedef::Motif;
use crate::rest::util::{ApiError, ApiResult, DataError};

impl From<CollectionModel> for Collection {
    fn from(model: CollectionModel) -> Self {
        Self {
            id: model.id,
            title: model.title,
            created_at: model.created_at.with_timezone(&Utc),
            updated_at: model.updated_at.with_timezone(&Utc),
            owner_id: model.owner_id,
        }
    }
}

pub async fn get_by_id(db: &DatabaseConnection, collection_id: Uuid) -> ApiResult<Collection> {
    let model = CollectionEntity::find_by_id(collection_id).one(db).await?;
    let collection = model.ok_or(DataError::NotFound("Collection not found".to_owned()))?;
    Ok(collection.into())
}

pub async fn get_by_owner_id(
    db: &DatabaseConnection,
    owner_id: Uuid,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Collection>> {
    let query = CollectionEntity::find()
        .filter(collections::Column::OwnerId.eq(owner_id))
        .opt_limit_offset(limit, offset);
    let collections: Vec<Collection> = query.all(db).await?.into_iter().map_into().collect();
    Ok(collections)
}

pub async fn get_motifs_by_id(
    db: &DatabaseConnection,
    collection_id: Uuid,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Motif>> {
    let join_with_motifs = CollectionMotifEntity::find()
        .find_with_related(MotifEntity)
        .filter(collection_motifs::Column::CollectionId.eq(collection_id))
        .opt_limit_offset(limit, offset)
        .all(db)
        .await?;

    let motifs: Vec<MotifModel> = join_with_motifs
        .into_iter()
        .map(|mut tuple| tuple.1.remove(0))
        // .flatten()
        .collect();

    let mapped: Vec<Motif> = motifs.into_iter().map_into().collect();
    Ok(mapped)
}

pub async fn delete_by_id(
    db: &DatabaseConnection,
    owner_id: Uuid,
    collection_id: Uuid,
) -> ApiResult<bool> {
    let existing = CollectionEntity::find_by_id(collection_id).one(db).await?;
    if let Some(existing) = existing {
        if existing.owner_id != owner_id {
            Err(ApiError::Authorization(
                "Must be owner of collection to delete".to_owned(),
            ))
        } else {
            existing.delete(db).await?;
            Ok(true)
        }
    } else {
        Ok(false)
    }
}

pub async fn create(
    db: &DatabaseConnection,
    owner_id: Uuid,
    input: CreateCollection,
) -> ApiResult<Collection> {
    let model = collections::ActiveModel {
        id: NotSet,
        title: Set(input.title),
        description: Set(input.description),
        created_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
        owner_id: Set(owner_id),
        updated_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
    };
    let motif = model.insert(db).await?;
    Ok(motif.into())
}

pub async fn is_owner(
    db: &DatabaseConnection,
    owner_id: Uuid,
    collection_id: Uuid,
) -> ApiResult<bool> {
    Ok(CollectionEntity::find()
        .filter(
            Condition::all()
                .add(collections::Column::OwnerId.eq(owner_id))
                .add(collections::Column::Id.eq(collection_id)),
        )
        .one(db)
        .await?
        .is_some())
}

async fn find_existing_member(
    db: &DatabaseConnection,
    collection_id: Uuid,
    motif_id: i32,
) -> ApiResult<Option<CollectionMotifModel>> {
    Ok(CollectionMotifEntity::find()
        .filter(
            Condition::all()
                .add(collection_motifs::Column::CollectionId.eq(collection_id))
                .add(collection_motifs::Column::MotifId.eq(motif_id)),
        )
        .one(db)
        .await?)
}

pub async fn add_motif_by_id(
    db: &DatabaseConnection,
    owner_id: Uuid,
    collection_id: Uuid,
    motif_id: i32,
) -> ApiResult<bool> {
    if !is_owner(db, owner_id, collection_id).await? {
        return Err(ApiError::Authorization(
            "Mut be owner of collection to add motif".to_owned(),
        ));
    }
    if find_existing_member(db, collection_id, motif_id)
        .await?
        .is_some()
    {
        return Ok(false);
    }
    let model = collection_motifs::ActiveModel {
        collection_id: Set(collection_id),
        motif_id: Set(motif_id),
    };
    model.insert(db).await?;
    Ok(true)
}

pub async fn remove_motif_by_id(
    db: &DatabaseConnection,
    owner_id: Uuid,
    collection_id: Uuid,
    motif_id: i32,
) -> ApiResult<bool> {
    if !is_owner(db, owner_id, collection_id).await? {
        return Err(ApiError::Authorization(
            "Must be owner of collection to remove motif".to_owned(),
        ));
    }
    let existing = find_existing_member(db, collection_id, motif_id).await?;
    if let Some(existing) = existing {
        existing.delete(db).await?;
        Ok(true)
    } else {
        Ok(false)
    }
}
