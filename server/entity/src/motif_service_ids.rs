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

use super::sea_orm_active_enums::Service;
use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "motif_service_ids")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: i32,
    pub motif_id: i32,
    pub service: Service,
    pub service_id: String,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(
        belongs_to = "super::motifs::Entity",
        from = "Column::MotifId",
        to = "super::motifs::Column::Id",
        on_update = "Cascade",
        on_delete = "Cascade"
    )]
    Motifs,
}

impl Related<super::motifs::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Motifs.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
