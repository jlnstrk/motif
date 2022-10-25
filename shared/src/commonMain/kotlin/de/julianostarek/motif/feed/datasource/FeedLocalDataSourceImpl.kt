package de.julianostarek.motif.feed.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import de.julianostarek.motif.feed.dto.FeedMotifDto
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