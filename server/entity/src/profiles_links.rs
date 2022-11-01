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
