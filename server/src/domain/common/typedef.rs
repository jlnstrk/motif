use async_graphql::Enum;
use serde::Serialize;

#[derive(Enum, Copy, Clone, Eq, PartialEq, Serialize)]
pub enum Service {
    Spotify,
    #[graphql(name = "APPLE_MUSIC")]
    AppleMusic,
}
