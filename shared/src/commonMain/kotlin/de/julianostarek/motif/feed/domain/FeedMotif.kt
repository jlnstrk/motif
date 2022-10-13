package de.julianostarek.motif.feed.domain

import kotlinx.datetime.Instant

data class FeedMotif(
    val id: Long,
    val spotifyTrackId: String,
    val offset: Int,
    val listened: Boolean,
    val createdAt: Instant
)