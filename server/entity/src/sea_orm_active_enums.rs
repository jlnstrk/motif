//! SeaORM Entity. Generated by sea-orm-codegen 0.9.3

use sea_orm::entity::prelude::*;

#[derive(Debug, Clone, PartialEq, EnumIter, DeriveActiveEnum)]
#[sea_orm(rs_type = "String", db_type = "Enum", enum_name = "service")]
pub enum Service {
    #[sea_orm(string_value = "APPLE_MUSIC")]
    AppleMusic,
    #[sea_orm(string_value = "SPOTIFY")]
    Spotify,
}
