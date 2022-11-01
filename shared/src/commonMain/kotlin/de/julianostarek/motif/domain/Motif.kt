package de.julianostarek.motif.domain

import kotlinx.datetime.Instant

sealed interface Motif {
    val id: Int
    val isrc: String
    val offset: Int
    val listened: Boolean
    val createdAt: Instant
    val creator: Profile

    data class Simple(
        override val id: Int,
        override val isrc: String,
        override val offset: Int,
        override val listened: Boolean,
        override val createdAt: Instant,
        override val creator: Profile
    ) : Motif

    data class Detail(
        override val id: Int,
        override val isrc: String,
        override val offset: Int,
        override val listened: Boolean,
        override val createdAt: Instant,
        override val creator: Profile,
        val listenersCount: Int,
        val listeners: List<Profile>,
        val liked: Boolean,
        val likesCount: Int,
        val likedBy: List<Profile>,
        val commentsCount: Int,
        val comments: List<Comment>
    ) : Motif
}