use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, EntityTrait, ModelTrait,
    PaginatorTrait, QueryFilter, QuerySelect,
};
use uuid::Uuid;

use entity::comment_likes;
use entity::comment_likes::{Entity as CommentLikeEntity, Model as CommentLikeModel};
use entity::motif_likes;
use entity::motif_likes::{Entity as MotifLikeEntity, Model as MotifLikeModel};
use entity::profiles::{Entity as ProfileEntity, Model as ProfileModel};

use crate::domain::profile::typedef::Profile;

pub async fn get_motif_likes_count(db: &DatabaseConnection, motif_id: i32) -> Result<i32, DbErr> {
    MotifLikeEntity::find()
        .filter(motif_likes::Column::MotifId.eq(motif_id))
        .count(db)
        .await
        .map(|count| count as i32)
}

pub async fn get_comment_likes_count(
    db: &DatabaseConnection,
    comment_id: i32,
) -> Result<i32, DbErr> {
    CommentLikeEntity::find()
        .filter(comment_likes::Column::CommentId.eq(comment_id))
        .count(db)
        .await
        .map(|count| count as i32)
}

pub async fn get_motif_likes(
    db: &DatabaseConnection,
    motif_id: i32,
    limit: Option<i32>,
    offset: Option<i32>,
) -> Result<Vec<Profile>, DbErr> {
    let mut query = MotifLikeEntity::find()
        .find_with_related(ProfileEntity)
        .filter(motif_likes::Column::MotifId.eq(motif_id));
    if let Some(limit) = limit {
        query = query.limit(limit as u64);
    }
    if let Some(offset) = offset {
        query = query.offset(offset as u64);
    }
    let likes_with_profiles = query.all(db).await?;

    let profiles: Vec<ProfileModel> = likes_with_profiles
        .into_iter()
        .map(|mut tuple| tuple.1.remove(0))
        // .flatten()
        .collect();

    let mapped: Vec<Profile> = profiles.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_comment_likes(
    db: &DatabaseConnection,
    comment_id: i32,
) -> Result<Vec<Profile>, DbErr> {
    let likes_with_profiles = CommentLikeEntity::find()
        .find_with_related(ProfileEntity)
        .filter(comment_likes::Column::CommentId.eq(comment_id))
        .all(db)
        .await?;

    let profiles: Vec<ProfileModel> = likes_with_profiles
        .into_iter()
        .map(|mut tuple| tuple.1.remove(0))
        // .flatten()
        .collect();

    let mapped: Vec<Profile> = profiles.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

async fn find_existing_motif_like(
    db: &DatabaseConnection,
    author_id: Uuid,
    motif_id: i32,
) -> Result<Option<MotifLikeModel>, DbErr> {
    MotifLikeEntity::find()
        .filter(
            Condition::all()
                .add(motif_likes::Column::LikerId.eq(author_id))
                .add(motif_likes::Column::MotifId.eq(motif_id)),
        )
        .one(db)
        .await
}

async fn find_existing_comment_like(
    db: &DatabaseConnection,
    author_id: Uuid,
    comment_id: i32,
) -> Result<Option<CommentLikeModel>, DbErr> {
    CommentLikeEntity::find()
        .filter(
            Condition::all()
                .add(comment_likes::Column::LikerId.eq(author_id))
                .add(comment_likes::Column::CommentId.eq(comment_id)),
        )
        .one(db)
        .await
}

pub async fn like_motif(
    db: &DatabaseConnection,
    author_id: Uuid,
    motif_id: i32,
) -> Result<bool, DbErr> {
    if find_existing_motif_like(db, author_id, motif_id)
        .await?
        .is_some()
    {
        return Ok(false);
    }
    let model = motif_likes::ActiveModel {
        motif_id: Set(motif_id),
        liker_id: Set(author_id),
    };
    model.insert(db).await?;
    Ok(true)
}

pub async fn unlike_motif(
    db: &DatabaseConnection,
    author_id: Uuid,
    motif_id: i32,
) -> Result<bool, DbErr> {
    let existing = find_existing_motif_like(db, author_id, motif_id).await?;
    if let Some(existing) = existing {
        existing.delete(db).await?;
        Ok(true)
    } else {
        Ok(false)
    }
}

pub async fn like_comment(
    db: &DatabaseConnection,
    author_id: Uuid,
    comment_id: i32,
) -> Result<bool, DbErr> {
    if find_existing_comment_like(db, author_id, comment_id)
        .await?
        .is_some()
    {
        return Ok(false);
    }
    let model = comment_likes::ActiveModel {
        comment_id: Set(comment_id),
        liker_id: Set(author_id),
    };
    model.insert(db).await?;
    Ok(true)
}

pub async fn unlike_comment(
    db: &DatabaseConnection,
    author_id: Uuid,
    comment_id: i32,
) -> Result<bool, DbErr> {
    let existing = find_existing_comment_like(db, author_id, comment_id).await?;
    if let Some(existing) = existing {
        existing.delete(db).await?;
        Ok(true)
    } else {
        Ok(false)
    }
}
