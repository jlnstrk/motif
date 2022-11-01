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

//! SeaORM Entity. Generated by sea-orm-codegen 0.9.3

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "profiles")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub user_id: Uuid,
    #[sea_orm(unique)]
    pub username: String,
    pub biography: Option<String>,
    pub photo_url: Option<String>,
    pub display_name: String,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(
        belongs_to = "super::users::Entity",
        from = "Column::UserId",
        to = "super::users::Column::Id",
        on_update = "Cascade",
        on_delete = "Cascade"
    )]
    Users,
    #[sea_orm(has_many = "super::collections::Entity")]
    Collections,
    #[sea_orm(has_many = "super::motifs::Entity")]
    Motifs,
    #[sea_orm(has_many = "super::motif_listeners::Entity")]
    MotifListeners,
    #[sea_orm(has_many = "super::comments::Entity")]
    Comments,
    #[sea_orm(has_many = "super::motif_likes::Entity")]
    MotifLikes,
    #[sea_orm(has_many = "super::comment_likes::Entity")]
    CommentLikes,
}

impl Related<super::users::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Users.def()
    }
}

impl Related<super::collections::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Collections.def()
    }
}

impl Related<super::motifs::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Motifs.def()
    }
}

impl Related<super::motif_listeners::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::MotifListeners.def()
    }
}

impl Related<super::comments::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Comments.def()
    }
}

impl Related<super::motif_likes::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::MotifLikes.def()
    }
}

impl Related<super::comment_likes::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::CommentLikes.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
