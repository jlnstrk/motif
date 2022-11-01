package de.julianostarek.motif.dto

import kotlinx.datetime.Instant

data class FeedMotifDto(
    val id: Int,
    val liked: Boolean,
    val listened: Boolean,
    val isrc: String,
    val offset: Int,
    val createdAt: Instant,
    val creatorId: String,
    val creatorUsername: String,
    val creatorDisplayName: String,
    val creatorPhotoUrl: String?
)