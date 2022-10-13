package de.julianostarek.motif.feed.domain

data class FeedMotifGroup(
    val creator: FeedCreator,
    val motifs: List<FeedMotif>
) {
    val isUnlistened: Boolean get() = motifs.firstOrNull()?.listened == false
}