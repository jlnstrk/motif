use anyhow::Context;
use sea_orm::sea_query::{OnConflict, SimpleExpr};
use sea_orm::ActiveValue::{Set, Unchanged};
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, DeriveColumn, EntityTrait,
    EnumIter, FromQueryResult, IntoActiveValue, Linked, NotSet, PaginatorTrait, QueryFilter,
    QuerySelect,
};
use serde::de::Error;
use uuid::Uuid;

use anyhow::Result;
use entity::profile_follows::Entity as ProfileFollowEntity;
use entity::profiles::{Entity as ProfileEntity, Model as ProfileModel};
use entity::profiles_links::{ProfileFollowToFollower, ProfileFollowToFollowing};
use entity::{profile_follows, profiles};
use sea_orm::IdenStatic;

use crate::domain::profile::typedef::{Profile, ProfileUpdate};

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

pub async fn get_by_id(db: &DatabaseConnection, profile_id: Uuid) -> Result<Profile> {
    let model = ProfileEntity::find_by_id(profile_id).one(db).await?;

    let model = model.context("Profile not found")?;
    Ok(model.into())
}

pub async fn get_by_username(db: &DatabaseConnection, username: String) -> Result<Profile> {
    let model = ProfileEntity::find()
        .filter(profiles::Column::Username.eq(username))
        .one(db)
        .await?;

    let model = model.context("Profile not found")?;
    Ok(model.into())
}

pub async fn search(db: &DatabaseConnection, query: String) -> Result<Vec<Profile>, DbErr> {
    let models = ProfileEntity::find()
        .filter(
            Condition::any()
                .add(profiles::Column::Username.contains(&query))
                .add(profiles::Column::DisplayName.contains(&query)),
        )
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
) -> Result<Vec<Profile>, DbErr> {
    let follows_with_profiles = ProfileFollowEntity::find()
        .find_also_linked(link)
        .filter(cond)
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
) -> Result<Vec<Profile>, DbErr> {
    get_profiles_from_follows(
        db,
        profile_follows::Column::FollowedId.eq(profile_id),
        ProfileFollowToFollower,
    )
    .await
}

pub async fn get_followers_count(db: &DatabaseConnection, profile_id: Uuid) -> Result<i64, DbErr> {
    ProfileFollowEntity::find()
        .filter(profile_follows::Column::FollowedId.eq(profile_id))
        .count(db)
        .await
        .map(|count| count as i64)
}

pub async fn get_following(
    db: &DatabaseConnection,
    profile_id: Uuid,
) -> Result<Vec<Profile>, DbErr> {
    get_profiles_from_follows(
        db,
        profile_follows::Column::FollowerId.eq(profile_id),
        ProfileFollowToFollowing,
    )
    .await
}

pub async fn get_following_ids(
    db: &DatabaseConnection,
    profile_id: Uuid,
) -> Result<Vec<Uuid>, DbErr> {
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

pub async fn get_following_count(db: &DatabaseConnection, profile_id: Uuid) -> Result<i64, DbErr> {
    ProfileFollowEntity::find()
        .filter(profile_follows::Column::FollowerId.eq(profile_id))
        .count(db)
        .await
        .map(|count| count as i64)
}

pub async fn update_by_id(
    db: &DatabaseConnection,
    profile_id: Uuid,
    update: ProfileUpdate,
) -> Result<Profile, DbErr> {
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
) -> Result<bool, DbErr> {
    let model = profile_follows::ActiveModel {
        follower_id: Set(follower_id),
        followed_id: Set(followed_id),
    };
    ProfileFollowEntity::insert(model)
        .on_conflict(OnConflict::new().do_nothing().to_owned())
        .exec(db)
        .await?;
    Ok(true)
}

pub async fn unfollow(
    db: &DatabaseConnection,
    follower_id: Uuid,
    followed_id: Uuid,
) -> Result<bool, DbErr> {
    let model = profile_follows::ActiveModel {
        follower_id: Set(follower_id),
        followed_id: Set(followed_id),
    };
    model.delete(db).await?;
    Ok(true)
}
