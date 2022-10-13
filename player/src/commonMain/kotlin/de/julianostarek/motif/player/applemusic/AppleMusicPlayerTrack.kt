package de.julianostarek.motif.player.applemusic

import de.julianostarek.motif.player.PlayerTrack
import kotlin.jvm.JvmInline

@JvmInline
public value class AppleMusicPlayerTrack(internal val backing: MusicPlayerMediaItem) : PlayerTrack {
    override val title: String
        get() = backing.title ?: "<Unknown>"
    override val album: String
        get() = backing.albumTitle ?: "<Unknown>"
    override val artists: List<String>
        get() = listOf(backing.artistName ?: "<Unknown>")
    override val duration: Long
        get() = backing.duration
}