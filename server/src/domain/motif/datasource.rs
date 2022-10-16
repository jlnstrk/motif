use chrono::{FixedOffset, Offset, TimeZone, Utc};
use sea_orm::sea_query::{OnConflict, Query};
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, EntityTrait, ModelTrait,
    NotSet, Order, PaginatorTrait, QueryFilter, QueryOrder, TransactionTrait,
};
use uuid::Uuid;

use entity::motif_listeners::Entity as MotifListenerEntity;
use entity::motif_service_ids::{Entity as MotifServiceIdEntity, Model as MotifServiceIdModel};
use entity::motifs::{Entity as MotifEntity, Model as MotifModel};
use entity::profile_follows::Entity as ProfileFollowEntity;
use entity::profiles::{Entity as ProfileEntity, Model as ProfileModel};
use entity::{motif_listeners, motif_service_ids, motifs, profile_follows};

use crate::domain::common::typedef::Service;
use crate::domain::motif::typedef::{CreateMotif, Motif, ServiceId};
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

impl From<MotifServiceIdModel> for ServiceId {
    fn from(model: MotifServiceIdModel) -> Self {
        Self {
            service: Service::from(model.service),
            id: model.service_id,
        }
    }
}

pub async fn get_by_id(db: &DatabaseConnection, motif_id: i32) -> ApiResult<Motif> {
    let model = MotifEntity::find_by_id(motif_id).one(db).await?;
    let motif = model.ok_or(DataError::NotFound("Motif not found".to_owned()))?;
    Ok(motif.into())
}

pub async fn get_service_ids_by_id(
    db: &DatabaseConnection,
    motif_id: i32,
) -> ApiResult<Vec<ServiceId>> {
    let models = MotifServiceIdEntity::find()
        .filter(motif_service_ids::Column::MotifId.eq(motif_id))
        .all(db)
        .await?;
    let mapped: Vec<ServiceId> = models.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_feed_by_profile_id(
    db: &DatabaseConnection,
    profile_id: Uuid,
) -> ApiResult<Vec<Motif>> {
    let models = MotifEntity::find()
        .filter(
            Condition::all()
                .add(
                    motifs::Column::CreatorId.in_subquery(
                        Query::select()
                            .column(profile_follows::Column::FollowedId)
                            .cond_where(profile_follows::Column::FollowerId.eq(profile_id))
                            .from(ProfileFollowEntity)
                            .to_owned(),
                    ),
                )
                .add(motifs::Column::CreatedAt.lt(Utc::now().with_timezone(&FixedOffset::east(0)))),
        )
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
) -> ApiResult<Vec<Profile>> {
    let models = MotifListenerEntity::find()
        .find_with_related(ProfileEntity)
        .filter(motif_listeners::Column::MotifId.eq(motif_id))
        .all(db)
        .await?;

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

            let service_id_models: Vec<motif_service_ids::ActiveModel> = input
                .service_ids
                .into_iter()
                .map(|id| motif_service_ids::ActiveModel {
                    id: NotSet,
                    motif_id: Set(motif.id),
                    service: Set(id.service.into()),
                    service_id: Set(id.id),
                })
                .collect();
            if !service_id_models.is_empty() {
                MotifServiceIdEntity::insert_many(service_id_models)
                    .exec(txn)
                    .await?;
            }

            Ok(motif.into())
        })
    })
    .await
    .map_err(|err| err.into())
}
