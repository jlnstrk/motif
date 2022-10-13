package de.julianostarek.motif.feed.datasource

import de.julianostarek.motif.backend.BackendClient
import de.julianostarek.motif.backend.GetMotifsFeedQuery
import de.julianostarek.motif.backend.OnMotifCreatedSubscription
import de.julianostarek.motif.backend.OnMotifDeletedSubscription
import de.julianostarek.motif.feed.dto.FeedMotifDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single
class FeedRemoteDataSourceImpl(
    private val backend: BackendClient,
    clientScope: CoroutineScope
) : FeedRemoteDataSource {
    override val motifCreated: Flow<FeedMotifDto> = backend.apollo.subscription(OnMotifCreatedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifCreated.toDto() }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())
    override val motifDeleted: Flow<Long> = backend.apollo.subscription(OnMotifDeletedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifDeleted }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())

    override fun motifsFeed(): Flow<List<FeedMotifDto>> {
        return backend.apollo.query(GetMotifsFeedQuery())
            .toFlow()
            .map { response -> response.dataAssertNoErrors.motifsFeed.map { it.toDto() } }
    }

    private fun OnMotifCreatedSubscription.MotifCreated.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
            listened = listened,
            spotifyTrackId = spotifyTrackId,
            offset = offset,
            createdAt = createdAt,
            creatorId = creator.id,
            creatorUsername = creator.username,
            creatorDisplayName = creator.displayName,
            creatorPhotoUrl = creator.photoUrl
        )
    }

    private fun GetMotifsFeedQuery.MotifsFeed.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
            listened = listened,
            spotifyTrackId = spotifyTrackId,
            offset = offset,
            createdAt = createdAt,
            creatorId = creator.id,
            creatorUsername = creator.username,
            creatorDisplayName = creator.displayName,
            creatorPhotoUrl = creator.photoUrl
        )
    }
}