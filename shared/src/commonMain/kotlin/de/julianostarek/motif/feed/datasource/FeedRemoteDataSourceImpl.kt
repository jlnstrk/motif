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
    override val motifCreated: Flow<FeedMotifDto> = backend.apollo.subscription(OnMotifCreatedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifCreated.toDto() }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())
    override val motifDeleted: Flow<Int> = backend.apollo.subscription(OnMotifDeletedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifDeleted }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())

    override fun motifsFeed(): Flow<List<FeedMotifDto>> {
        return backend.apollo.query(GetMotifMyFeedQuery())
            .toFlow()
            .map { response -> response.dataAssertNoErrors.motifMyFeed.map { it.toDto() } }
    }

    private fun OnMotifCreatedSubscription.MotifCreated.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
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

    private fun GetMotifMyFeedQuery.MotifMyFeed.toDto(): FeedMotifDto {
        return FeedMotifDto(
            id = id,
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