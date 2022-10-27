package de.julianostarek.motif.feed.datasource

import de.julianostarek.motif.dto.FeedMotifDto
import kotlinx.coroutines.flow.Flow

interface FeedRemoteDataSource : FeedDataSource {
    val motifCreated: Flow<FeedMotifDto>
    val motifDeleted: Flow<Int>
}