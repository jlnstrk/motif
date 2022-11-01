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

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import de.julianostarek.motif.dto.FeedMotifDto
import de.julianostarek.motif.persist.MotifDatabase
import de.julianostarek.motif.persist.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class FeedLocalDataSourceImpl(
    private val database: MotifDatabase,
    private val externalScope: CoroutineScope
) : FeedLocalDataSource {

    override fun motifsFeed(): Flow<List<FeedMotifDto>> {
        return database.motifQueries
            .selectUnlistened()
            .asFlow()
            .mapToList(externalScope.coroutineContext)
            .map { selectUnlisteneds -> selectUnlisteneds.map { it.toDto() } }
    }

    override suspend fun dumpMotifsFeed(motifsFeed: List<FeedMotifDto>) {
        val ids = motifsFeed.map { it.id.toLong() }
        database.transaction {
            database.motifQueries.deleteExceptIds(ids)
            motifsFeed.forEach { motif ->
                database.motifQueries.upsert(motif.toMotifEntity())
                database.profileQueries.upsert(motif.toProfileEntity())
            }
        }
    }

    override suspend fun insertMotif(motif: FeedMotifDto) {
        database.transaction {
            database.motifQueries.upsert(motif.toMotifEntity())
            database.profileQueries.upsert(motif.toProfileEntity())
        }
    }

    override suspend fun deleteMotif(motifId: Int) {
        database.motifQueries.deleteById(motifId.toLong())
    }

    private fun SelectUnlistened.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id.toInt(),
            liked = liked,
            listened = listened,
            isrc = spotifyTrackId,
            offset = offset,
            createdAt = createdAt,
            creatorId = creatorId,
            creatorUsername = username,
            creatorDisplayName = displayName,
            creatorPhotoUrl = photoUrl
        )
    }

    private fun FeedMotifDto.toMotifEntity(): MotifEntity {
        return MotifEntity(
            id = id.toLong(),
            spotifyTrackId = isrc,
            offset = offset,
            createdAt = createdAt,
            liked = liked,
            listened = listened,
            creatorId = creatorId
        )
    }

    private fun FeedMotifDto.toProfileEntity(): ProfileEntity {
        return ProfileEntity(
            id = creatorId,
            displayName = creatorDisplayName,
            username = creatorUsername,
            photoUrl = creatorPhotoUrl
        )
    }
}