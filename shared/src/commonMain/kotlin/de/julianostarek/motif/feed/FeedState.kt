package de.julianostarek.motif.feed

import de.julianostarek.motif.feed.domain.ProfileWithMotifs

sealed class FeedState {
    object NotLoading : FeedState()
    object Loading : FeedState()
    data class Data(
        val motifGroups: List<ProfileWithMotifs>,
        val refreshing: Boolean = false
    ) : FeedState()
}