package de.julianostarek.motif.domain

import kotlinx.datetime.Instant

data class Comment(
    val id: Int,
    val createdAt: Instant,
    val creator: Profile,
    val text: String,
    val likesCount: Int,
    val likedBy: List<Profile>?,
    val commentsCount: Int,
    val comments: List<Comment>?
)