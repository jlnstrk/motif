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

package de.julianostarek.motif.graphql

import de.julianostarek.motif.client.FeedProfilesQuery
import de.julianostarek.motif.domain.Metadata
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.domain.ProfileWithMotifs

fun FeedProfilesQuery.Node.toProfileWithMotifs(): ProfileWithMotifs {
    return ProfileWithMotifs(
        profile = toSimpleProfile(),
        motifs = feed.map { it.toSimpleMotif() },
    )
}

fun FeedProfilesQuery.Node.toSimpleProfile(): Profile.Simple {
    return Profile.Simple(
        id = id,
        username = username,
        displayName = displayName,
        photoUrl = photoUrl,
    )
}

fun FeedProfilesQuery.Feed.toSimpleMotif(): Motif.Simple {
    return Motif.Simple(
        id = id,
        liked = liked,
        listened = listened,
        isrc = isrc,
        offset = offset,
        createdAt = createdAt,
        creator = null,
        metadata = metadata?.toMetadata()
    )
}

fun FeedProfilesQuery.Metadata.toMetadata(): Metadata {
    return Metadata(
        name = name,
        artist = artist,
        coverArtUrl = coverArtUrl
    )
}