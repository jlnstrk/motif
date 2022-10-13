use std::error::Error;

use chrono::{FixedOffset, Utc};
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, EntityTrait, ModelTrait,
    NotSet, QueryFilter,
};
use uuid::Uuid;

use entity::collection_motifs::{Entity as CollectionMotifEntity, Model as CollectionMotifModel};
use entity::collections::{Entity as CollectionEntity, Model as CollectionModel};
use entity::motifs::{Entity as MotifEntity, Model as MotifModel};
use entity::{collection_motifs, collections};

use crate::domain::collection::typedef::{Collection, CreateCollection};
use crate::domain::motif::typedef::Motif;
use anyhow::{anyhow, Context, Result};

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

pub async fn get_by_id(db: &DatabaseConnection, collection_id: Uuid) -> Result<Collection> {
    let model = CollectionEntity::find_by_id(collection_id).one(db).await?;
    let collection = model.context("Collection not found")?;
    Ok(collection.into())
}

pub async fn get_by_owner_id(db: &DatabaseConnection, owner_id: Uuid) -> Result<Vec<Collection>> {
    let models = CollectionEntity::find()
        .filter(collections::Column::OwnerId.eq(owner_id))
        .all(db)
        .await?;
    let mapped: Vec<Collection> = models.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_motifs_by_id(db: &DatabaseConnection, collection_id: Uuid) -> Result<Vec<Motif>> {
    let join_with_motifs = CollectionMotifEntity::find()
        .find_with_related(MotifEntity)
        .filter(collection_motifs::Column::CollectionId.eq(collection_id))
        .all(db)
        .await?;

    let motifs: Vec<MotifModel> = join_with_motifs
        .into_iter()
        .map(|mut tuple| tuple.1.remove(0))
        // .flatten()
        .collect();

    let mapped: Vec<Motif> = motifs.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn delete_by_id(
    db: &DatabaseConnection,
    owner_id: Uuid,
    collection_id: Uuid,
) -> Result<bool> {
    let existing = CollectionEntity::find_by_id(collection_id).one(db).await?;
    if let Some(existing) = existing {
        if existing.owner_id != owner_id {
            Err(anyhow!("Not owner of collection"))
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
) -> Result<Collection> {
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
) -> Result<bool> {
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
) -> Result<Option<CollectionMotifModel>> {
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
) -> Result<bool> {
    if !is_owner(db, owner_id, collection_id).await? {
        return Err(anyhow!("Not owner of collection"));
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
) -> Result<bool> {
    if !is_owner(db, owner_id, collection_id).await? {
        return Err(anyhow!("Not owner of collection"));
    }
    let existing = find_existing_member(db, collection_id, motif_id).await?;
    if let Some(existing) = existing {
        existing.delete(db).await?;
        Ok(true)
    } else {
        Ok(false)
    }
}
