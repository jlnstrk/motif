package de.julianostarek.motif.feed.datasource

import de.julianostarek.motif.feed.domain.FeedMotif
import de.julianostarek.motif.feed.dto.FeedMotifDto
import kotlinx.coroutines.flow.Flow

interface FeedDataSource {
    fun motifsFeed(): Flow<List<FeedMotifDto>>
}