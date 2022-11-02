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

package de.julianostarek.motif.util

import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.persist.entity.MotifEntity
import de.julianostarek.motif.persist.entity.ProfileEntity
import de.julianostarek.motif.persist.entity.SelectUnlistened

fun ProfileEntity.toSimple(): Profile.Simple {
    return Profile.Simple(
        displayName = displayName,
        id = id,
        photoUrl = photoUrl,
        username = username
    )
}

fun Profile.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        displayName = displayName,
        username = username,
        photoUrl = photoUrl
    )
}

fun SelectUnlistened.toSimple(): Motif.Simple {
    return Motif.Simple(
        id = id.toInt(),
        liked = liked,
        listened = listened,
        isrc = spotifyTrackId,
        offset = offset,
        createdAt = createdAt,
        creator = Profile.Simple(
            id = creatorId,
            username = username,
            displayName = displayName,
            photoUrl = photoUrl
        )
    )
}

fun Motif.toEntity(): MotifEntity {
    return MotifEntity(
        id = id.toLong(),
        spotifyTrackId = isrc,
        offset = offset,
        createdAt = createdAt,
        liked = liked,
        listened = listened,
        creatorId = creator.id
    )
}