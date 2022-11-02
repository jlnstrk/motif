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

import com.apollographql.apollo3.api.Optional
import de.julianostarek.motif.client.*
import de.julianostarek.motif.client.type.CreateMotif
import de.julianostarek.motif.client.type.ProfileUpdate
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.dto.MotifCreateDto
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.profileedit.ProfileEdit

fun MotifCreateDto.toCreateMotif(): CreateMotif {
    return CreateMotif(
        isrc = isrc,
        serviceIds = emptyList(),
        offset = offset
    )
}

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
        )
    )
}

fun MotifMyFeedQuery.MotifMyFeed.toSimple(): Motif.Simple {
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
        )
    )
}

fun MotifCreateMutation.MotifCreate.toDetail(): Motif.Detail {
    return Motif.Detail(
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
        listenersCount = listenersCount,
        listeners = emptyList(),
        likesCount = likesCount,
        likedBy = likes.nodes.map(MotifCreateMutation.Node::toSimple),
        commentsCount = commentsCount,
        comments = emptyList()
    )
}

fun MotifCreateMutation.Node.toSimple(): Profile.Simple {
    return Profile.Simple(
        displayName = displayName,
        id = id,
        photoUrl = photoUrl,
        username = username,
    )
}

fun ProfileSearchQuery.ProfileSearch.toSimple(): Profile.Simple {
    return Profile.Simple(
        displayName = displayName,
        id = id,
        photoUrl = photoUrl,
        username = username,
    )
}

fun ProfileMeUpdatedSubscription.ProfileMe.toDetail(): Profile.Detail {
    return Profile.Detail(
        displayName = displayName,
        id = id,
        username = username,
        photoUrl = photoUrl,
        biography = biography,
        follows = false,
        followersCount = followersCount,
        followingCount = followingCount
    )
}

fun ProfileMeQuery.ProfileMe.toDetail(): Profile.Detail {
    return Profile.Detail(
        displayName = displayName,
        id = id,
        username = username,
        photoUrl = photoUrl,
        biography = biography,
        follows = false,
        followersCount = followersCount,
        followingCount = followingCount
    )
}

fun ProfileByIdQuery.ProfileById.toDetail(): Profile.Detail {
    return Profile.Detail(
        displayName = displayName,
        id = id,
        username = username,
        photoUrl = photoUrl,
        biography = biography,
        follows = follows,
        followersCount = followersCount,
        followingCount = followingCount
    )
}

fun ProfileEdit.toUpdate(): ProfileUpdate {
    return ProfileUpdate(
        displayName = Optional.presentIfNotNull(displayName),
        username = Optional.presentIfNotNull(username),
        photoUrl = Optional.absent(),
        biography = Optional.presentIfNotNull(biography)
    )
}