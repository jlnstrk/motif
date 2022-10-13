package de.julianostarek.motif.player.spotify

import de.julianostarek.motif.player.PlayerTrack
import kotlin.jvm.JvmInline

@JvmInline
public value class SpotifyPlayerTrack(internal val backing: Track) : PlayerTrack {
    override val title: String
        get() = backing.name
    override val album: String
        get() = backing.album.name
    override val artists: List<String>
        get() = backing.artists.map(Artist::name)
    override val duration: Long
        get() = backing.duration
}