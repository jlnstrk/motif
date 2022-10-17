//! SeaORM Entity. Generated by sea-orm-codegen 0.9.3

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "motifs")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub isrc: String,
    pub offset: i32,
    pub created_at: DateTimeWithTimeZone,
    pub creator_id: Uuid,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(
        belongs_to = "super::profiles::Entity",
        from = "Column::CreatorId",
        to = "super::profiles::Column::UserId",
        on_update = "Cascade",
        on_delete = "Cascade"
    )]
    Profiles,
    #[sea_orm(has_many = "super::collection_motifs::Entity")]
    CollectionMotifs,
    #[sea_orm(has_many = "super::motif_service_ids::Entity")]
    MotifServiceIds,
    #[sea_orm(has_many = "super::motif_listeners::Entity")]
    MotifListeners,
    #[sea_orm(has_many = "super::comments::Entity")]
    Comments,
    #[sea_orm(has_many = "super::motif_likes::Entity")]
    MotifLikes,
}

impl Related<super::profiles::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Profiles.def()
    }
}

impl Related<super::collection_motifs::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::CollectionMotifs.def()
    }
}

impl Related<super::motif_service_ids::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::MotifServiceIds.def()
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

impl ActiveModelBehavior for ActiveModel {}