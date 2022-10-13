package de.julianostarek.motif.feed.domain

data class FeedCreator(
    val id: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?
)