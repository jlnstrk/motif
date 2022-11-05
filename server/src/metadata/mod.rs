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

use std::error::Error;
use std::time::Duration;

use apalis::prelude::{Job, JobContext, JobError, JobResult, Storage};
use apalis::redis::RedisStorage;
use axum::http::Method;
use chrono::{FixedOffset, Utc};
use fred::bytes::Buf;
use log::{error, info};
use reqwest::header::{ACCEPT, USER_AGENT};
use reqwest::StatusCode;
use sea_orm::ActiveValue::Set;
use sea_orm::IdenStatic;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, DatabaseConnection, DeriveColumn, EntityTrait, EnumIter,
    JoinType, QueryFilter, QuerySelect, TransactionTrait,
};
use serde::{Deserialize, Serialize};
use serde_xml_rs::from_reader;
use tokio::time::sleep;

use entity::{isrc_metadata, isrc_metadata_status, motifs};

mod coverartarchive;
mod musicbrainz;

#[derive(Debug, Default, Clone, Deserialize, Serialize)]
pub struct FetchMetadata {
    pub isrc: String,
}

impl Job for FetchMetadata {
    const NAME: &'static str = "motif::FetchMetadata";
}

#[derive(Debug, Default, Clone, Deserialize, Serialize)]
pub struct ScheduleFetchMetadata {}

impl Job for ScheduleFetchMetadata {
    const NAME: &'static str = "motif::ScheduleFetchMetadata";
}

const MUSICBRAINZ_BASE_URL: &str = "https://musicbrainz.org/ws/2/";
const COVER_ART_ARCHIVE_BASE_URL: &str = "https://coverartarchive.org/release/";

pub async fn schedule_fetch_metadata(
    _schedule: ScheduleFetchMetadata,
    ctx: JobContext,
) -> Result<JobResult, JobError> {
    info!("Scheduling metadata fetch for ISRCs without status");

    let db: &DatabaseConnection = ctx.data_opt().unwrap();
    let mut storage: RedisStorage<FetchMetadata> = ctx
        .data_opt::<RedisStorage<FetchMetadata>>()
        .unwrap()
        .clone();

    #[derive(Copy, Clone, Debug, EnumIter, DeriveColumn)]
    enum QueryAs {
        Isrc,
    }

    let isrcs_to_fetch = db
        .transaction::<_, Vec<String>, JobError>(|txn| {
            Box::pin(async move {
                let isrcs_to_schedule: Vec<String> = motifs::Entity::find()
                    .select_only()
                    .distinct()
                    .column_as(motifs::Column::Isrc, QueryAs::Isrc)
                    .join(
                        JoinType::LeftJoin,
                        motifs::Entity::belongs_to(isrc_metadata_status::Entity)
                            .from(motifs::Column::Isrc)
                            .to(isrc_metadata_status::Column::Isrc)
                            .into(),
                    )
                    .filter(isrc_metadata_status::Column::Isrc.is_null())
                    .into_values::<_, QueryAs>()
                    .all(txn)
                    .await
                    .unwrap();

                for isrc in &isrcs_to_schedule {
                    isrc_metadata_status::ActiveModel {
                        isrc: Set(isrc.clone()),
                        updated_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
                    }
                    .insert(txn)
                    .await
                    .unwrap();
                }

                Ok(isrcs_to_schedule)
            })
        })
        .await
        .unwrap();

    for isrc in &isrcs_to_fetch {
        storage.push(FetchMetadata { isrc: isrc.clone() }).await?;
    }

    if !isrcs_to_fetch.is_empty() {
        info!(
            "Scheduled metadata fetch for ISRCs: {}",
            isrcs_to_fetch.join(", ")
        );
    } else {
        info!("Nothing to schedule: All ISRCs have a metadata status");
    }

    Ok(JobResult::Success)
}

pub async fn fetch_metadata(
    metadata: FetchMetadata,
    ctx: JobContext,
) -> Result<JobResult, JobError> {
    info!("Fetching metadata for ISRC: {}", &metadata.isrc);

    // Sleep 1s to avoid MusicBrainz' per-IP rate limit
    sleep(Duration::from_secs(1)).await;

    let db: &DatabaseConnection = ctx.data_opt().unwrap();
    let client = reqwest::Client::new();

    // Fetch general metadata from MusicBrainz
    let mb_metadata = musicbrainz_isrc_lookup(&client, &metadata.isrc)
        .await
        .map_err(|err| JobError::Failed(err))?;

    if mb_metadata.is_none() {
        info!("Did not find metadata for ISRC: {}", &metadata.isrc);
        return Ok(JobResult::Success);
    }
    let mb_metadata = mb_metadata.unwrap();

    // Fetch cover art url from CoverArtArchive
    let cover_url = cover_art_archive_lookup(&client, &mb_metadata.release_mbids)
        .await
        .map_err(|err| JobError::Failed(err))?;

    let model = isrc_metadata::ActiveModel {
        isrc: Set(metadata.isrc.clone()),
        name: Set(mb_metadata.name),
        artist: Set(mb_metadata.artist),
        cover_art_url: Set(cover_url),
    };

    match model.insert(db).await {
        Ok(_) => {
            info!("Successfully updated metadata for ISRC: {}", &metadata.isrc);
            Ok(JobResult::Success)
        }
        Err(err) => {
            error!("Job failed: DB error: {}", err);
            Err(JobError::Unknown)
        }
    }
}

struct MusicBrainzMetadata {
    mbid: String,
    name: String,
    artist: String,
    release_mbids: Vec<String>,
}

async fn musicbrainz_isrc_lookup(
    client: &reqwest::Client,
    isrc: &String,
) -> Result<Option<MusicBrainzMetadata>, Box<dyn Error + Send + Sync>> {
    let request = client
        .request(
            Method::GET,
            format!("{}isrc/{}", MUSICBRAINZ_BASE_URL, isrc),
        )
        .query(&[("inc", "artists+releases")])
        .header(ACCEPT, "application/xml")
        .header(USER_AGENT, "de.julianostarek.motif")
        .build()
        .unwrap();

    let response = client.execute(request).await?;
    if response.status() == StatusCode::NOT_FOUND {
        return Ok(None);
    }

    let metadata_response: musicbrainz::Metadata = from_reader(response.bytes().await?.reader())?;
    let recording = metadata_response
        .isrc
        .recording_list
        .recording
        .first()
        .unwrap();
    let metadata = MusicBrainzMetadata {
        mbid: recording.id.clone(),
        name: recording.title.clone(),
        artist: recording
            .artist_credit
            .name_credit
            .first()
            .unwrap()
            .artist
            .name
            .clone(),
        release_mbids: recording
            .release_list
            .release
            .iter()
            .map(|release| release.id.clone())
            .collect(),
    };
    Ok(Some(metadata))
}

async fn cover_art_archive_lookup(
    client: &reqwest::Client,
    mbids: &Vec<String>,
) -> Result<Option<String>, Box<dyn Error + Send + Sync>> {
    for id in mbids {
        let request = client
            .request(Method::GET, format!("{}{}", COVER_ART_ARCHIVE_BASE_URL, id))
            .header(ACCEPT, "application/json")
            .build()
            .unwrap();

        let response = client.execute(request).await?;
        if response.status() != StatusCode::OK {
            continue;
        }
        let caa_response: coverartarchive::Response = response.json().await?;
        let image = caa_response.images.first().map(|img| img.image.clone());
        return Ok(image);
    }
    Ok(None)
}
