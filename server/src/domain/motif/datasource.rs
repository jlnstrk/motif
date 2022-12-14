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

use std::collections::{HashMap, HashSet};

use chrono::{DateTime, FixedOffset, Offset, Utc};
use sea_orm::sea_query::OnConflict;
use sea_orm::ActiveValue::Set;
use sea_orm::IdenStatic;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, DeriveColumn, EntityTrait,
    EnumIter, ModelTrait, NotSet, Order, PaginatorTrait, QueryFilter, QueryOrder, QuerySelect,
    TransactionTrait,
};
use uuid::Uuid;

use db::util::OptLimitOffset;
use entity::isrc_metadata::{Entity as MetadataEntity, Model as MetadataModel};
use entity::isrc_services::{Entity as IsrcServiceEntity, Model as IsrcServiceModel};
use entity::motif_listeners::Entity as MotifListenerEntity;
use entity::motifs::{Entity as MotifEntity, Model as MotifModel};
use entity::profiles::{Entity as ProfileEntity, Model as ProfileModel};
use entity::{isrc_metadata, isrc_services, motif_listeners, motifs};

use crate::db;
use crate::domain::common::typedef::Service;
use crate::domain::motif::typedef::{CreateMotif, Metadata, Motif, ServiceId};
use crate::domain::profile::typedef::Profile;
use crate::rest::util::{ApiError, ApiResult, DataError};

impl From<MotifModel> for Motif {
    fn from(model: MotifModel) -> Self {
        Self {
            id: model.id,
            isrc: model.isrc,
            offset: model.offset,
            created_at: model.created_at.with_timezone(&Utc),
            creator_id: model.creator_id,
        }
    }
}

impl From<IsrcServiceModel> for ServiceId {
    fn from(model: IsrcServiceModel) -> Self {
        Self {
            service: Service::from(model.service),
            id: model.service_id,
        }
    }
}

impl From<MetadataModel> for Metadata {
    fn from(model: MetadataModel) -> Self {
        Self {
            name: model.name,
            artist: model.artist,
            cover_art_url: model.cover_art_url,
        }
    }
}

pub async fn get_by_id(db: &DatabaseConnection, motif_id: i32) -> ApiResult<Motif> {
    let model = MotifEntity::find_by_id(motif_id).one(db).await?;
    let motif = model.ok_or(DataError::NotFound("Motif not found".to_owned()))?;
    Ok(motif.into())
}

pub async fn get_by_creator_id(
    db: &DatabaseConnection,
    creator_id: Uuid,
    after: Option<DateTime<Utc>>,
    before: Option<DateTime<Utc>>,
    limit: Option<u64>,
) -> ApiResult<Vec<Motif>> {
    let mut query = MotifEntity::find()
        .filter(motifs::Column::CreatorId.eq(creator_id))
        .order_by(motifs::Column::CreatedAt, Order::Desc);
    if let Some(after) = after {
        query = query.filter(motifs::Column::CreatedAt.lt(after));
    }
    if let Some(before) = before {
        query = query.filter(motifs::Column::CreatedAt.gt(before));
    }
    if let Some(limit) = limit {
        query = query.limit(limit);
    }
    let models = query.all(db).await?;
    let mapped: Vec<Motif> = models.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_service_ids_by_isrc(
    db: &DatabaseConnection,
    isrc: String,
) -> ApiResult<Vec<ServiceId>> {
    let models = IsrcServiceEntity::find()
        .filter(isrc_services::Column::Isrc.eq(isrc))
        .all(db)
        .await?;
    let mapped: Vec<ServiceId> = models.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_public_feed(db: &DatabaseConnection) -> ApiResult<Vec<Motif>> {
    let models = MotifEntity::find()
        .order_by(motifs::Column::CreatedAt, Order::Desc)
        .all(db)
        .await?;
    let mapped: Vec<Motif> = models.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_listeners_count_by_id(db: &DatabaseConnection, motif_id: i32) -> ApiResult<i32> {
    MotifListenerEntity::find()
        .filter(motif_listeners::Column::MotifId.eq(motif_id))
        .count(db)
        .await
        .map_err(|err| err.into())
        .map(|count| count as i32)
}

pub async fn get_listeners_by_id(
    db: &DatabaseConnection,
    motif_id: i32,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Profile>> {
    let query = MotifListenerEntity::find()
        .find_with_related(ProfileEntity)
        .filter(motif_listeners::Column::MotifId.eq(motif_id))
        .opt_limit_offset(limit, offset);
    let models = query.all(db).await?;

    let profile_models: Vec<ProfileModel> = models
        .into_iter()
        .map(|mut model| model.1.remove(0))
        // .flatten()
        .collect();

    let profiles: Vec<Profile> = profile_models
        .into_iter()
        .map(|model| model.into())
        .collect();

    Ok(profiles)
}

pub async fn has_listened(
    db: &DatabaseConnection,
    profile_id: Uuid,
    motif_id: i32,
) -> Result<bool, ApiError> {
    Ok(MotifListenerEntity::find()
        .filter(
            Condition::all()
                .add(motif_listeners::Column::ListenerId.eq(profile_id))
                .add(motif_listeners::Column::MotifId.eq(motif_id)),
        )
        .one(db)
        .await?
        .is_some())
}

pub async fn has_listened_all(
    db: &DatabaseConnection,
    profile_id: Uuid,
    motif_ids: &[i32],
) -> Result<HashSet<i32>, ApiError> {
    #[derive(Copy, Clone, Debug, EnumIter, DeriveColumn)]
    enum QueryAs {
        MotifId,
    }
    let ids: Vec<i32> = MotifListenerEntity::find()
        .select_only()
        .column_as(motif_listeners::Column::MotifId, QueryAs::MotifId)
        .filter(
            Condition::all()
                .add(motif_listeners::Column::ListenerId.eq(profile_id))
                .add(motif_listeners::Column::MotifId.is_in(motif_ids.to_vec())),
        )
        .into_values::<_, QueryAs>()
        .all(db)
        .await?;
    Ok(ids.into_iter().collect())
}

pub async fn get_metadata_all(
    db: &DatabaseConnection,
    isrcs: &[String],
) -> Result<HashMap<String, Metadata>, ApiError> {
    let models: Vec<MetadataModel> = MetadataEntity::find()
        .filter(isrc_metadata::Column::Isrc.is_in(isrcs.to_owned()))
        .all(db)
        .await?;
    let mapped: HashMap<String, Metadata> = models
        .into_iter()
        .map(|model| (model.isrc.clone(), model.into()))
        .collect();
    Ok(mapped)
}

pub async fn listen_by_id(
    db: &DatabaseConnection,
    listener_id: Uuid,
    motif_id: i32,
) -> ApiResult<bool> {
    if MotifListenerEntity::find()
        .filter(
            Condition::all()
                .add(motif_listeners::Column::ListenerId.eq(listener_id))
                .add(motif_listeners::Column::MotifId.eq(motif_id)),
        )
        .one(db)
        .await?
        .is_some()
    {
        return Ok(false);
    }

    let now = Utc::now();
    let model = motif_listeners::ActiveModel {
        motif_id: Set(motif_id),
        listener_id: Set(listener_id),
        listened_at: Set(now.with_timezone(&now.timezone().fix())),
    };
    MotifListenerEntity::insert(model)
        .on_conflict(OnConflict::new().do_nothing().to_owned())
        .exec(db)
        .await?;
    Ok(true)
}

pub async fn delete_by_id(
    db: &DatabaseConnection,
    creator_id: Uuid,
    motif_id: i32,
) -> ApiResult<bool> {
    let existing = MotifEntity::find_by_id(motif_id).one(db).await?;
    if let Some(existing) = existing {
        if existing.creator_id != creator_id {
            Err(ApiError::Authorization(
                "Must be creator of motif to delete".to_owned(),
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
    creator_id: Uuid,
    input: CreateMotif,
) -> ApiResult<Motif> {
    db.transaction::<_, Motif, DbErr>(|txn| {
        Box::pin(async move {
            let model = motifs::ActiveModel {
                id: NotSet,
                isrc: Set(input.isrc),
                offset: Set(input.offset),
                created_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
                creator_id: Set(creator_id),
            };
            let motif = model.insert(txn).await?;

            let service_id_models: Vec<isrc_services::ActiveModel> = input
                .service_ids
                .into_iter()
                .map(|id| isrc_services::ActiveModel {
                    id: NotSet,
                    isrc: Set(motif.isrc.clone()),
                    service: Set(id.service.into()),
                    service_id: Set(id.id),
                })
                .collect();
            if !service_id_models.is_empty() {
                IsrcServiceEntity::insert_many(service_id_models)
                    .on_conflict(
                        OnConflict::columns([
                            isrc_services::Column::Isrc,
                            isrc_services::Column::Service,
                        ])
                        .do_nothing()
                        .to_owned(),
                    )
                    .exec(txn)
                    .await?;
            }

            Ok(motif.into())
        })
    })
    .await
    .map_err(|err| err.into())
}
