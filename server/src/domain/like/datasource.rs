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

use itertools::Itertools;
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, DeriveColumn, EntityTrait,
    EnumIter, ModelTrait, PaginatorTrait, QueryFilter, QuerySelect,
};
use std::collections::HashSet;
use uuid::Uuid;

use entity::comment_likes;
use entity::comment_likes::{Entity as CommentLikeEntity, Model as CommentLikeModel};
use entity::motif_likes;
use entity::motif_likes::{Entity as MotifLikeEntity, Model as MotifLikeModel};
use entity::profiles::{Entity as ProfileEntity, Model as ProfileModel};

use crate::db::util::OptLimitOffset;
use crate::domain::profile::typedef::Profile;
use crate::rest::util::ApiError;
use sea_orm::IdenStatic;

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
    limit: Option<u64>,
    offset: Option<u64>,
) -> Result<Vec<Profile>, DbErr> {
    let query = MotifLikeEntity::find()
        .find_with_related(ProfileEntity)
        .filter(motif_likes::Column::MotifId.eq(motif_id))
        .opt_limit_offset(limit, offset);
    let likes_with_profiles = query.all(db).await?;

    let profiles: Vec<ProfileModel> = likes_with_profiles
        .into_iter()
        .map(|tuple| tuple.1.into_iter().next())
        .flatten()
        .collect();

    let mapped: Vec<Profile> = profiles.into_iter().map_into().collect();
    Ok(mapped)
}

pub async fn get_comment_likes(
    db: &DatabaseConnection,
    comment_id: i32,
    limit: Option<u64>,
    offset: Option<u64>,
) -> Result<Vec<Profile>, DbErr> {
    let likes_with_profiles = CommentLikeEntity::find()
        .find_with_related(ProfileEntity)
        .filter(comment_likes::Column::CommentId.eq(comment_id))
        .opt_limit_offset(limit, offset)
        .all(db)
        .await?;

    let profiles: Vec<ProfileModel> = likes_with_profiles
        .into_iter()
        .map(|tuple| tuple.1.into_iter().next())
        .flatten()
        .collect();

    let mapped: Vec<Profile> = profiles.into_iter().map_into().collect();
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

pub async fn has_liked_motif_all(
    db: &DatabaseConnection,
    profile_id: Uuid,
    motif_ids: &[i32],
) -> Result<HashSet<i32>, ApiError> {
    #[derive(Copy, Clone, Debug, EnumIter, DeriveColumn)]
    enum QueryAs {
        MotifId,
    }
    let ids: Vec<i32> = MotifLikeEntity::find()
        .select_only()
        .column_as(motif_likes::Column::MotifId, QueryAs::MotifId)
        .filter(
            Condition::all()
                .add(motif_likes::Column::LikerId.eq(profile_id))
                .add(motif_likes::Column::MotifId.is_in(motif_ids.to_vec())),
        )
        .into_values::<_, QueryAs>()
        .all(db)
        .await?;
    Ok(ids.into_iter().collect())
}

pub async fn has_liked_comment_all(
    db: &DatabaseConnection,
    profile_id: Uuid,
    comment_ids: &[i32],
) -> Result<HashSet<i32>, ApiError> {
    #[derive(Copy, Clone, Debug, EnumIter, DeriveColumn)]
    enum QueryAs {
        CommentId,
    }
    let ids: Vec<i32> = CommentLikeEntity::find()
        .select_only()
        .column_as(comment_likes::Column::CommentId, QueryAs::CommentId)
        .filter(
            Condition::all()
                .add(comment_likes::Column::LikerId.eq(profile_id))
                .add(comment_likes::Column::CommentId.is_in(comment_ids.to_vec())),
        )
        .into_values::<_, QueryAs>()
        .all(db)
        .await?;
    Ok(ids.into_iter().collect())
}
