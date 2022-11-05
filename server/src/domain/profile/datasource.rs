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

use sea_orm::sea_query::SimpleExpr;
use sea_orm::ActiveValue::{Set, Unchanged};
use sea_orm::IdenStatic;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DeriveColumn, EntityTrait,
    EnumIter, IntoActiveValue, Linked, NotSet, PaginatorTrait, QueryFilter, QuerySelect,
};
use std::collections::HashSet;
use uuid::Uuid;

use crate::db::util::OptLimitOffset;
use entity::profile_follows::Entity as ProfileFollowEntity;
use entity::profiles::{Entity as ProfileEntity, Model as ProfileModel};
use entity::profiles_links::{ProfileFollowToFollower, ProfileFollowToFollowing};
use entity::{profile_follows, profiles};

use crate::domain::profile::typedef::{Profile, ProfileUpdate};
use crate::rest::util::{ApiError, ApiResult, DataError};

impl From<ProfileModel> for Profile {
    fn from(model: ProfileModel) -> Self {
        Self {
            id: model.user_id,
            display_name: model.display_name,
            username: model.username,
            photo_url: model.photo_url,
            biography: model.biography,
        }
    }
}

pub async fn get_by_id(db: &DatabaseConnection, profile_id: Uuid) -> ApiResult<Profile> {
    let model = ProfileEntity::find_by_id(profile_id).one(db).await?;

    let model = model.ok_or(DataError::NotFound("Profile not found".to_owned()))?;
    Ok(model.into())
}

pub async fn get_by_username(db: &DatabaseConnection, username: String) -> ApiResult<Profile> {
    let model = ProfileEntity::find()
        .filter(profiles::Column::Username.eq(username))
        .one(db)
        .await?;

    let model = model.ok_or(DataError::NotFound("Profile not found".to_owned()))?;
    Ok(model.into())
}

pub async fn search(
    db: &DatabaseConnection,
    query: String,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Profile>> {
    let models = ProfileEntity::find()
        .filter(
            Condition::any()
                .add(profiles::Column::Username.contains(&query))
                .add(profiles::Column::DisplayName.contains(&query)),
        )
        .opt_limit_offset(limit, offset)
        .all(db)
        .await?;

    let mapped: Vec<Profile> = models.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

async fn get_profiles_from_follows<
    L: Linked<FromEntity = ProfileFollowEntity, ToEntity = ProfileEntity>,
>(
    db: &DatabaseConnection,
    cond: SimpleExpr,
    link: L,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Profile>> {
    let follows_with_profiles = ProfileFollowEntity::find()
        .find_also_linked(link)
        .filter(cond)
        .opt_limit_offset(limit, offset)
        .all(db)
        .await?;

    let profiles: Vec<ProfileModel> = follows_with_profiles
        .into_iter()
        .map(|tuple| tuple.1)
        .flatten()
        .collect();

    let mapped: Vec<Profile> = profiles.into_iter().map(|model| model.into()).collect();
    Ok(mapped)
}

pub async fn get_followers(
    db: &DatabaseConnection,
    profile_id: Uuid,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Profile>> {
    get_profiles_from_follows(
        db,
        profile_follows::Column::FollowedId.eq(profile_id),
        ProfileFollowToFollower,
        limit,
        offset,
    )
    .await
}

pub async fn get_followers_count(db: &DatabaseConnection, profile_id: Uuid) -> ApiResult<i64> {
    ProfileFollowEntity::find()
        .filter(profile_follows::Column::FollowedId.eq(profile_id))
        .count(db)
        .await
        .map_err(|err| err.into())
        .map(|count| count as i64)
}

pub async fn get_following(
    db: &DatabaseConnection,
    profile_id: Uuid,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Profile>> {
    get_profiles_from_follows(
        db,
        profile_follows::Column::FollowerId.eq(profile_id),
        ProfileFollowToFollowing,
        limit,
        offset,
    )
    .await
}

pub async fn get_following_ids(db: &DatabaseConnection, profile_id: Uuid) -> ApiResult<Vec<Uuid>> {
    #[derive(Copy, Clone, Debug, EnumIter, DeriveColumn)]
    enum QueryAs {
        FollowedId,
    }

    let ids: Vec<Uuid> = ProfileFollowEntity::find()
        .select_only()
        .column_as(profile_follows::Column::FollowedId, QueryAs::FollowedId)
        .filter(profile_follows::Column::FollowerId.eq(profile_id))
        .into_values::<_, QueryAs>()
        .all(db)
        .await?;
    Ok(ids)
}

pub async fn get_following_count(db: &DatabaseConnection, profile_id: Uuid) -> ApiResult<i64> {
    ProfileFollowEntity::find()
        .filter(profile_follows::Column::FollowerId.eq(profile_id))
        .count(db)
        .await
        .map_err(|err| err.into())
        .map(|count| count as i64)
}

pub async fn update_by_id(
    db: &DatabaseConnection,
    profile_id: Uuid,
    update: ProfileUpdate,
) -> ApiResult<Profile> {
    let model = profiles::ActiveModel {
        user_id: Unchanged(profile_id),
        username: if let Some(username) = update.username {
            Set(username)
        } else {
            NotSet
        },
        biography: update.biography.into_active_value(),
        photo_url: update.photo_url.into_active_value(),
        display_name: if let Some(display_name) = update.display_name {
            Set(display_name)
        } else {
            NotSet
        },
    };

    let updated: Profile = model.update(db).await?.into();
    Ok(updated)
}

pub async fn follow(
    db: &DatabaseConnection,
    follower_id: Uuid,
    followed_id: Uuid,
) -> ApiResult<bool> {
    let existing = ProfileFollowEntity::find()
        .filter(
            Condition::all()
                .add(profile_follows::Column::FollowerId.eq(follower_id))
                .add(profile_follows::Column::FollowedId.eq(followed_id)),
        )
        .one(db)
        .await?;
    if existing.is_none() {
        let model = profile_follows::ActiveModel {
            follower_id: Set(follower_id),
            followed_id: Set(followed_id),
        };
        ProfileFollowEntity::insert(model).exec(db).await?;
    }
    Ok(existing.is_none())
}

pub async fn unfollow(
    db: &DatabaseConnection,
    follower_id: Uuid,
    followed_id: Uuid,
) -> ApiResult<bool> {
    let model = profile_follows::ActiveModel {
        follower_id: Set(follower_id),
        followed_id: Set(followed_id),
    };
    model.delete(db).await?;
    Ok(true)
}

pub async fn is_username_available(db: &DatabaseConnection, username: String) -> ApiResult<bool> {
    let count = ProfileEntity::find()
        .filter(profiles::Column::Username.eq(username))
        .count(db)
        .await?;
    Ok(count == 0)
}

pub async fn follows_all(
    db: &DatabaseConnection,
    profile_id: Uuid,
    other_profile_ids: &[Uuid],
) -> Result<HashSet<Uuid>, ApiError> {
    #[derive(Copy, Clone, Debug, EnumIter, DeriveColumn)]
    enum QueryAs {
        FollowedId,
    }
    let ids: Vec<Uuid> = ProfileFollowEntity::find()
        .select_only()
        .column_as(profile_follows::Column::FollowedId, QueryAs::FollowedId)
        .filter(
            Condition::all()
                .add(profile_follows::Column::FollowerId.eq(profile_id))
                .add(profile_follows::Column::FollowedId.is_in(other_profile_ids.to_vec())),
        )
        .into_values::<_, QueryAs>()
        .all(db)
        .await?;
    Ok(ids.into_iter().collect())
}
