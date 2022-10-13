use crate::{profile_follows, profiles};
use sea_orm::{Linked, RelationDef, RelationTrait};

#[derive(Debug)]
pub struct ProfileToFollower;

impl Linked for ProfileToFollower {
    type FromEntity = profiles::Entity;

    type ToEntity = profiles::Entity;

    fn link(&self) -> Vec<RelationDef> {
        vec![
            profile_follows::Relation::Profiles2.def().rev(),
            profile_follows::Relation::Profiles1.def(),
        ]
    }
}

#[derive(Debug)]
pub struct ProfileToFollowing;

impl Linked for ProfileToFollowing {
    type FromEntity = profiles::Entity;

    type ToEntity = profiles::Entity;

    fn link(&self) -> Vec<RelationDef> {
        vec![
            profile_follows::Relation::Profiles1.def().rev(),
            profile_follows::Relation::Profiles2.def(),
        ]
    }
}

#[derive(Debug)]
pub struct ProfileFollowToFollower;

impl Linked for ProfileFollowToFollower {
    type FromEntity = profile_follows::Entity;

    type ToEntity = profiles::Entity;

    fn link(&self) -> Vec<RelationDef> {
        vec![profile_follows::Relation::Profiles2.def()]
    }
}

#[derive(Debug)]
pub struct ProfileFollowToFollowing;

impl Linked for ProfileFollowToFollowing {
    type FromEntity = profile_follows::Entity;

    type ToEntity = profiles::Entity;

    fn link(&self) -> Vec<RelationDef> {
        vec![profile_follows::Relation::Profiles1.def()]
    }
}
