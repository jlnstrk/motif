package de.julianostarek.motif.feed

import de.julianostarek.motif.domain.ProfileWithMotifs

sealed class FeedState {
    object NotLoading : FeedState()
    object Loading : FeedState()
    data class Data(
        val profilesGrid: SquareGrid<ProfileWithMotifs?>,
        val refreshing: Boolean = false
    ) : FeedState()
}