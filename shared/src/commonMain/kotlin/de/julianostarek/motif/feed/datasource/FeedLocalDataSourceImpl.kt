package de.julianostarek.motif.feed.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import de.julianostarek.motif.feed.dto.FeedMotifDto
import de.julianostarek.motif.persist.MotifDatabase
import de.julianostarek.motif.persist.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class FeedLocalDataSourceImpl(
    private val database: MotifDatabase
) : FeedLocalDataSource {

    override fun motifsFeed(): Flow<List<FeedMotifDto>> {
        return database.motifQueries
            .selectUnlistened()
            .asFlow()
            .mapToList()
            .map { selectUnlisteneds -> selectUnlisteneds.map { it.toDto() } }
    }

    override suspend fun dumpMotifsFeed(motifsFeed: List<FeedMotifDto>) {
        val ids = motifsFeed.map(FeedMotifDto::id)
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

    override suspend fun deleteMotif(motifId: Long) {
        database.motifQueries.deleteById(motifId)
    }

    private fun SelectUnlistened.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
            listened = listened,
            spotifyTrackId = spotifyTrackId,
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
            id = id,
            spotifyTrackId = spotifyTrackId,
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