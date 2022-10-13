use std::error::Error;

use anyhow::{anyhow, Context, Result};
use chrono::{FixedOffset, Utc};
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, DatabaseConnection, DbErr, EntityTrait, ModelTrait, NotSet,
    PaginatorTrait, QueryFilter,
};
use uuid::Uuid;

use entity::comments;
use entity::comments::{Entity as CommentEntity, Model as CommentModel};

use crate::domain::comment::typedef::{Comment, CreateComment};

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

pub async fn get_by_id(db: &DatabaseConnection, comment_id: i32) -> Result<Comment> {
    let model = CommentEntity::find_by_id(comment_id).one(db).await?;
    let comment: Comment = model
        .map(|model| model.into())
        .context("Comment does not exist")?;
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
) -> Result<Vec<Comment>> {
    let models = CommentEntity::find()
        .filter(comments::Column::MotifId.eq(motif_id))
        .all(db)
        .await?;

    let mapped: Vec<Comment> = models.into_iter().map(|model| model.into()).collect();

    Ok(mapped)
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
) -> Result<Vec<Comment>, DbErr> {
    let models = CommentEntity::find()
        .filter(comments::Column::ParentId.eq(parent_comment_id))
        .all(db)
        .await?;

    let mapped: Vec<Comment> = models.into_iter().map(|model| model.into()).collect();

    Ok(mapped)
}

pub async fn delete_by_id(
    db: &DatabaseConnection,
    author_id: Uuid,
    comment_id: i32,
) -> Result<bool> {
    let existing = CommentEntity::find_by_id(comment_id).one(db).await?;
    if let Some(existing) = existing {
        if existing.author_id != author_id {
            Err(anyhow!("Not creator of motif"))
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
) -> Result<Comment> {
    let motif_id = if let Some(value) = motif_id {
        value
    } else if let Some(parent_id) = parent_id {
        CommentEntity::find_by_id(parent_id)
            .one(db)
            .await?
            .context("Parent does not exist")?
            .motif_id
    } else {
        return Err(anyhow!("Must set either motif_id or parent_id"));
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
