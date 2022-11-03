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

use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct Metadata {
    pub isrc: Isrc,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub struct Isrc {
    pub id: String,
    pub recording_list: RecordingList,
}

#[derive(Debug, Deserialize)]
pub struct RecordingList {
    pub recording: Vec<Recording>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub struct Recording {
    pub id: String,
    pub title: String,
    pub artist_credit: ArtistCredit,
    pub release_list: ReleaseList,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub struct ArtistCredit {
    pub name_credit: Vec<NameCredit>,
}

#[derive(Debug, Deserialize)]
pub struct NameCredit {
    pub artist: Artist,
}

#[derive(Debug, Deserialize)]
pub struct Artist {
    pub name: String,
}

#[derive(Debug, Deserialize)]
pub struct ReleaseList {
    pub release: Vec<Release>,
}

#[derive(Debug, Deserialize)]
pub struct Release {
    pub id: String,
    pub title: String,
}
