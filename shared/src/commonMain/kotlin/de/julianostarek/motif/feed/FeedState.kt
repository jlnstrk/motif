package de.julianostarek.motif.feed

import de.julianostarek.motif.feed.domain.FeedMotifGroup

sealed class FeedState {
    object NotLoading : FeedState()
    object Loading : FeedState()
    data class Data(
        val motifGroups: List<FeedMotifGroup>,
        val refreshing: Boolean = false
    ) : FeedState()
}