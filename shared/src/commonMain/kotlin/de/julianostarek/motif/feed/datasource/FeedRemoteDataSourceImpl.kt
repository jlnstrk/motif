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

package de.julianostarek.motif.feed.datasource

import de.julianostarek.motif.client.*
import de.julianostarek.motif.dto.FeedMotifDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single
class FeedRemoteDataSourceImpl(
    private val backend: BackendClient,
    clientScope: CoroutineScope
) : FeedRemoteDataSource {
    override val motifCreated: Flow<FeedMotifDto> = backend.apollo.subscription(MotifCreatedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifCreated.toDto() }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())
    override val motifDeleted: Flow<Int> = backend.apollo.subscription(MotifDeletedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifDeleted }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())

    override fun motifsFeed(): Flow<List<FeedMotifDto>> {
        return backend.apollo.query(MotifMyFeedQuery())
            .toFlow()
            .map { response -> response.dataAssertNoErrors.motifMyFeed.map { it.toDto() } }
    }

    private fun MotifCreatedSubscription.MotifCreated.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
            liked = liked,
            listened = listened,
            isrc = isrc,
            offset = offset,
            createdAt = createdAt,
            creatorId = creator.id,
            creatorUsername = creator.username,
            creatorDisplayName = creator.displayName,
            creatorPhotoUrl = creator.photoUrl
        )
    }

    private fun MotifMyFeedQuery.MotifMyFeed.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
            liked = liked,
            listened = listened,
            isrc = isrc,
            offset = offset,
            createdAt = createdAt,
            creatorId = creator.id,
            creatorUsername = creator.username,
            creatorDisplayName = creator.displayName,
            creatorPhotoUrl = creator.photoUrl
        )
    }
}