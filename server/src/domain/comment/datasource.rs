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
    ActiveModelTrait, ColumnTrait, DatabaseConnection, DbErr, EntityTrait, ModelTrait, NotSet,
    PaginatorTrait, QueryFilter,
};
use uuid::Uuid;

use crate::db::util::OptLimitOffset;
use entity::comments;
use entity::comments::{Entity as CommentEntity, Model as CommentModel};

use crate::domain::comment::typedef::{Comment, CreateComment};
use crate::rest::util::{ApiError, ApiResult, DataError, GeneralError};

impl From<CommentModel> for Comment {
    fn from(model: CommentModel) -> Self {
        Self {
            id: model.id,
            text: model.content,
            offset: model.offset,
            created_at: model.created_at.with_timezone(&Utc),
            author_id: model.author_id,
            motif_id: model.motif_id,
            parent_comment_id: model.parent_id,
        }
    }
}

pub async fn get_by_id(db: &DatabaseConnection, comment_id: i32) -> ApiResult<Comment> {
    let model = CommentEntity::find_by_id(comment_id).one(db).await?;
    let comment: Comment = model
        .map(|model| model.into())
        .ok_or(DataError::NotFound("Comment not found".to_owned()))?;
    Ok(comment)
}

pub async fn get_motif_comments_count_by_id(
    db: &DatabaseConnection,
    motif_id: i32,
) -> Result<i32, DbErr> {
    CommentEntity::find()
        .filter(comments::Column::MotifId.eq(motif_id))
        .count(db)
        .await
        .map(|count| count as i32)
}

pub async fn get_motif_comments_by_id(
    db: &DatabaseConnection,
    motif_id: i32,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Comment>> {
    let query = CommentEntity::find()
        .filter(comments::Column::MotifId.eq(motif_id))
        .opt_limit_offset(limit, offset);
    let comments = query.all(db).await?.into_iter().map_into().collect();
    Ok(comments)
}

pub async fn get_child_comments_count_by_id(
    db: &DatabaseConnection,
    parent_comment_id: i32,
) -> Result<i32, DbErr> {
    CommentEntity::find()
        .filter(comments::Column::ParentId.eq(parent_comment_id))
        .count(db)
        .await
        .map(|count| count as i32)
}

pub async fn get_child_comments_by_id(
    db: &DatabaseConnection,
    parent_comment_id: i32,
    limit: Option<u64>,
    offset: Option<u64>,
) -> Result<Vec<Comment>, DbErr> {
    let query = CommentEntity::find()
        .filter(comments::Column::ParentId.eq(parent_comment_id))
        .opt_limit_offset(limit, offset);
    let comments = query.all(db).await?.into_iter().map_into().collect();
    Ok(comments)
}

pub async fn delete_by_id(
    db: &DatabaseConnection,
    author_id: Uuid,
    comment_id: i32,
) -> ApiResult<bool> {
    let existing = CommentEntity::find_by_id(comment_id).one(db).await?;
    if let Some(existing) = existing {
        if existing.author_id != author_id {
            Err(ApiError::Authorization("Not creator of motif".to_owned()))
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
    author_id: Uuid,
    motif_id: Option<i32>,
    parent_id: Option<i32>,
    input: CreateComment,
) -> ApiResult<Comment> {
    let motif_id = if let Some(value) = motif_id {
        value
    } else if let Some(parent_id) = parent_id {
        CommentEntity::find_by_id(parent_id)
            .one(db)
            .await?
            .ok_or(DataError::NotFound("Parent does not exist".to_owned()))?
            .motif_id
    } else {
        return Err(ApiError::General(GeneralError::Internal));
    };
    let model = comments::ActiveModel {
        id: NotSet,
        motif_id: Set(motif_id),
        parent_id: Set(parent_id),
        offset: Set(input.offset),
        content: Set(input.text),
        author_id: Set(author_id),
        created_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
    };
    let comment = model.insert(db).await?;
    Ok(comment.into())
}
