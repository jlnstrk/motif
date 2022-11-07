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

import de.julianostarek.motif.client.MotifDetailQuery
import de.julianostarek.motif.domain.Metadata
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.Profile

fun MotifDetailQuery.MotifById.toDetail(): Motif.Detail {
    return Motif.Detail(
        id = id,
        isrc = isrc,
        offset = offset,
        liked = liked,
        listened = listened,
        createdAt = createdAt,
        creator = creator.toSimple(),
        metadata = metadata?.toMetadata(),
        listenersCount = listenersCount,
        listenersPhotoUrls = listeners.nodes.map { it.photoUrl },
        likesCount = likesCount,
        likedByPhotoUrls = likes.nodes.map { it.photoUrl },
        commentsCount = commentsCount,
    )
}

fun MotifDetailQuery.Creator.toSimple(): Profile.Simple {
    return Profile.Simple(
        displayName = displayName,
        photoUrl = photoUrl,
        id = id,
        username = username,
    )
}

fun MotifDetailQuery.Metadata.toMetadata(): Metadata {
    return Metadata(
        name = name,
        artist = artist,
        coverArtUrl = coverArtUrl,
    )
}