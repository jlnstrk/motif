package de.julianostarek.motif.feed.domain

sealed interface Profile {
    val id: String
    val username: String
    val displayName: String
    val photoUrl: String?

    data class Simple(
        override val id: String,
        override val username: String,
        override val displayName: String,
        override val photoUrl: String?
    ) : Profile

    data class Detail(
        override val id: String,
        override val username: String,
        override val displayName: String,
        override val photoUrl: String?,
        val followersCount: Int,
        val followingCount: Int,
        val biography: String?,
    ) : Profile
}