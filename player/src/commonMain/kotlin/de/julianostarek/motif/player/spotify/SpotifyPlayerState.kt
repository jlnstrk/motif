package de.julianostarek.motif.player.spotify

import kotlin.jvm.JvmInline
import de.julianostarek.motif.player.PlayerState
import de.julianostarek.motif.player.PlayerTrack

@JvmInline
public value class SpotifyPlayerState(internal val backing: de.julianostarek.motif.player.spotify.PlayerState) :
    PlayerState {
    override val track: PlayerTrack?
        get() = backing.track?.let(::SpotifyPlayerTrack)
    override val state: PlayerState.PlaybackState
        get() = if (backing.isPaused) PlayerState.PlaybackState.PAUSED else PlayerState.PlaybackState.PLAYING
    override val position: Long
        get() = backing.playbackPosition
    override val repeatMode: PlayerState.RepeatMode
        get() = when (backing.playbackOptions?.repeatMode) {
            RepeatMode.OFF -> PlayerState.RepeatMode.OFF
            RepeatMode.TRACK -> PlayerState.RepeatMode.ONE
            RepeatMode.CONTEXT -> PlayerState.RepeatMode.ALL
            null -> throw IllegalArgumentException()
        }
    override val shuffleMode: PlayerState.ShuffleMode
        get() = if (backing.playbackOptions?.isShuffling == true) PlayerState.ShuffleMode.ON else PlayerState.ShuffleMode.OFF
}