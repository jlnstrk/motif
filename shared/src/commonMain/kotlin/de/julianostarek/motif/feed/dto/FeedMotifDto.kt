package de.julianostarek.motif.feed.dto

import kotlinx.datetime.Instant

data class FeedMotifDto(
    val id: Long,
    val listened: Boolean,
    val spotifyTrackId: String,
    val offset: Int,
    val createdAt: Instant,
    val creatorId: String,
    val creatorUsername: String,
    val creatorDisplayName: String,
    val creatorPhotoUrl: String?
)