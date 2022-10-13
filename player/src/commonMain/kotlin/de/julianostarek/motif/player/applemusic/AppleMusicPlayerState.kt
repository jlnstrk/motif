package de.julianostarek.motif.player.applemusic

import de.julianostarek.motif.player.PlayerState
import de.julianostarek.motif.player.PlayerTrack
import kotlin.jvm.JvmInline

@JvmInline
public value class AppleMusicPlayerState(internal val backing: MusicPlayerController) : PlayerState {
    override val track: PlayerTrack?
        get() = backing.currentItem?.let(::AppleMusicPlayerTrack)
    override val state: PlayerState.PlaybackState
        get() = when (backing.playbackState) {
            PlaybackState.STOPPED -> PlayerState.PlaybackState.STOPPED
            PlaybackState.PAUSED,
            PlaybackState.INTERRUPTED -> PlayerState.PlaybackState.PAUSED

            PlaybackState.PLAYING,
            PlaybackState.SEEKING_BACKWARD,
            PlaybackState.SEEKING_FORWARD -> PlayerState.PlaybackState.PLAYING
        }
    override val position: Long
        get() = backing.playbackPosition
    override val repeatMode: PlayerState.RepeatMode
        get() = when (backing.repeatMode) {
            RepeatMode.OFF -> PlayerState.RepeatMode.OFF
            RepeatMode.ONE -> PlayerState.RepeatMode.ONE
            RepeatMode.ALL -> PlayerState.RepeatMode.ALL
            RepeatMode.DEFAULT -> PlayerState.RepeatMode.OFF
        }
    override val shuffleMode: PlayerState.ShuffleMode
        get() = when (backing.shuffleMode) {
            ShuffleMode.OFF -> PlayerState.ShuffleMode.OFF
            ShuffleMode.SONGS,
            ShuffleMode.ALBUMS -> PlayerState.ShuffleMode.ON

            ShuffleMode.DEFAULT -> PlayerState.ShuffleMode.OFF
        }
}