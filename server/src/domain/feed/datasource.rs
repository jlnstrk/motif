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

use chrono::{DateTime, FixedOffset, Utc};
use itertools::Itertools;
use sea_orm::sea_query::{Alias, Query, SelectStatement};
use sea_orm::{
    ColumnTrait, Condition, ConnectionTrait, DatabaseConnection, EntityTrait, Iterable, Order,
    QueryFilter, QueryOrder, QuerySelect, QueryTrait,
};
use std::collections::HashMap;
use uuid::Uuid;

use entity::{motifs, profile_follows, profiles};
use motifs::Entity as MotifEntity;
use profile_follows::Entity as ProfileFollowEntity;
use profiles::Entity as ProfileEntity;

use crate::db::util::OptLimitOffset;
use crate::domain::motif::typedef::Motif;
use crate::domain::profile::typedef::Profile;
use crate::rest::util::ApiResult;

pub async fn get_motifs_by_profile_id(
    db: &DatabaseConnection,
    profile_id: Uuid,
    after: Option<DateTime<Utc>>,
    before: Option<DateTime<Utc>>,
    limit: Option<u64>,
) -> ApiResult<Vec<Motif>> {
    let mut query = MotifEntity::find()
        .filter(
            Condition::all()
                .add(
                    motifs::Column::CreatorId.in_subquery(
                        Query::select()
                            .column(profile_follows::Column::FollowedId)
                            .cond_where(profile_follows::Column::FollowerId.eq(profile_id))
                            .from(ProfileFollowEntity)
                            .to_owned(),
                    ),
                )
                .add(motifs::Column::CreatedAt.lt(Utc::now().with_timezone(&FixedOffset::east(0)))),
        )
        .order_by(motifs::Column::CreatedAt, Order::Desc)
        .opt_limit_offset(limit, None);
    if let Some(after) = after {
        query = query.filter(motifs::Column::CreatedAt.lt(after));
    }
    if let Some(before) = before {
        query = query.filter(motifs::Column::CreatedAt.gt(before));
    }
    let models = query.all(db).await?.into_iter().map_into().collect();
    Ok(models)
}

pub async fn get_profiles_by_profile_id(
    db: &DatabaseConnection,
    profile_id: Uuid,
    limit: Option<u64>,
    offset: Option<u64>,
) -> ApiResult<Vec<Profile>> {
    let subquery: SelectStatement = ProfileEntity::find()
        .distinct_on([profiles::Column::UserId])
        .inner_join(MotifEntity)
        .column(motifs::Column::CreatedAt)
        .filter(
            motifs::Column::CreatorId.in_subquery(
                Query::select()
                    .column(profile_follows::Column::FollowedId)
                    .from(ProfileFollowEntity)
                    .cond_where(profile_follows::Column::FollowerId.eq(profile_id))
                    .to_owned(),
            ),
        )
        .order_by(profiles::Column::UserId, Order::Desc)
        .order_by(motifs::Column::CreatedAt, Order::Desc)
        .opt_limit_offset(limit, offset)
        .as_query()
        .to_owned();
    let select: SelectStatement = Query::select()
        .columns(profiles::Column::iter())
        .from_subquery(subquery, Alias::new("subquery"))
        .order_by(motifs::Column::CreatedAt, Order::Desc)
        .to_owned();

    let stmt = db.get_database_backend().build(&select);
    let query = profiles::Entity::find().from_raw_sql(stmt);
    let models = query.all(db).await?.into_iter().map_into().collect();
    Ok(models)
}

pub async fn get_motifs_by_profile_ids(
    db: &DatabaseConnection,
    profile_ids: &[Uuid],
) -> ApiResult<HashMap<Uuid, Vec<Motif>>> {
    let models = MotifEntity::find()
        .filter(motifs::Column::CreatorId.is_in(profile_ids.to_vec()))
        .order_by(motifs::Column::CreatedAt, Order::Desc)
        .all(db)
        .await?;
    let map: HashMap<Uuid, Vec<Motif>> = models
        .into_iter()
        .map_into()
        .into_group_map_by(|motif: &Motif| motif.creator_id);
    Ok(map)
}
