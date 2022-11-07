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

import de.julianostarek.motif.client.MotifCreatedSubscription
import de.julianostarek.motif.domain.Metadata
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.Profile

fun MotifCreatedSubscription.MotifCreated.toSimple(): Motif.Simple {
    return Motif.Simple(
        id = id,
        liked = liked,
        listened = listened,
        isrc = isrc,
        offset = offset,
        createdAt = createdAt,
        creator = Profile.Simple(
            id = creator.id,
            username = creator.username,
            displayName = creator.displayName,
            photoUrl = creator.photoUrl
        ),
        metadata = metadata?.toMetadata()
    )
}

fun MotifCreatedSubscription.Metadata.toMetadata(): Metadata {
    return Metadata(
        name = name,
        artist = artist,
        coverArtUrl = coverArtUrl,
    )
}