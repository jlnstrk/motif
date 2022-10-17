package de.julianostarek.motif.feed

import de.julianostarek.motif.feed.domain.FeedMotifGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FeedRepository {
    val isFeedRefreshing: StateFlow<Boolean>
    fun motifsFeed(): Flow<List<FeedMotifGroup>>
}