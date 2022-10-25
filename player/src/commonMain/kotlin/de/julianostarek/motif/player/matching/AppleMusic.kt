package de.julianostarek.motif.player.matching

import kotlinx.serialization.Serializable

@Serializable
internal class AppleMusicResponse(
    val data: List<AppleMusicSong> = emptyList()
)

@Serializable
internal class AppleMusicSong(
    val id: String
) {

    @Serializable
    internal class Attributes(
        val isrc: String
    )
}