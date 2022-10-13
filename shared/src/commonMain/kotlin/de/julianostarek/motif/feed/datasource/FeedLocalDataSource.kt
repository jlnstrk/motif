package de.julianostarek.motif.feed.datasource

import de.julianostarek.motif.feed.dto.FeedMotifDto

interface FeedLocalDataSource : FeedDataSource {
    suspend fun dumpMotifsFeed(motifsFeed: List<FeedMotifDto>)
    suspend fun insertMotif(motif: FeedMotifDto)
    suspend fun deleteMotif(motifId: Long)
}