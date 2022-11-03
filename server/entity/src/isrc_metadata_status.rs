//! SeaORM Entity. Generated by sea-orm-codegen 0.9.3

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "isrc_metadata_status")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub isrc: String,
    pub updated_at: DateTimeWithTimeZone,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(
        belongs_to = "super::isrc_metadata::Entity",
        from = "Column::Isrc",
        to = "super::isrc_metadata::Column::Isrc",
        on_update = "NoAction",
        on_delete = "NoAction"
    )]
    IsrcMetadata,
}

impl Related<super::isrc_metadata::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::IsrcMetadata.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
