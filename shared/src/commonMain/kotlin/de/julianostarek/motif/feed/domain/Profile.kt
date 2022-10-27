package de.julianostarek.motif.feed.domain

sealed interface Profile {
    val displayName: String
    val id: String
    val photoUrl: String?
    val username: String

    data class Simple(
        override val displayName: String,
        override val photoUrl: String?,
        override val id: String,
        override val username: String,
    ) : Profile

    data class Detail(
        override val displayName: String,
        override val id: String,
        override val photoUrl: String?,
        override val username: String,
        val biography: String?,
        val followersCount: Int,
        val followingCount: Int,
    ) : Profile
}