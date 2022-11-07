package de.julianostarek.motif.player

import de.julianostarek.motif.player.applemusic.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.jvm.JvmInline

@JvmInline
public value class AppleMusicPlayer(internal val backing: MusicPlayerController) : Player {
    override val platform: PlatformControls get() = AppleMusicPlatformControls()

    override suspend fun resume() {
        backing.play()
    }

    override suspend fun pause() {
        backing.pause()
    }

    override suspend fun stop() {
        backing.stop()
    }

    override suspend fun getPlayerState(): PlayerState {
        return AppleMusicPlayerState(backing)
    }

    override suspend fun playerState(): Flow<PlayerState> {
        val playbackState = backing.playbackStateChanged()
        val currentItem = backing.currentItemChanged()
        return combine(playbackState, currentItem) { _, _ ->
            AppleMusicPlayerState(backing)
        }
    }

    override suspend fun seekTo(position: Long) {
        backing.playbackPosition = position
    }

    override suspend fun setRepeatMode(repeatMode: PlayerState.RepeatMode) {
        backing.repeatMode = when (repeatMode) {
            PlayerState.RepeatMode.OFF -> RepeatMode.OFF
            PlayerState.RepeatMode.ONE -> RepeatMode.ONE
            PlayerState.RepeatMode.ALL -> RepeatMode.ALL
        }
    }

    override suspend fun setShuffleMode(shuffleMode: PlayerState.ShuffleMode) {
        backing.shuffleMode = when (shuffleMode) {
            PlayerState.ShuffleMode.OFF -> ShuffleMode.OFF
            PlayerState.ShuffleMode.ON -> ShuffleMode.SONGS
        }
    }
}

public expect class AppleMusicPlatformControls() : PlatformControls

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
    override val url: String?
        get() = backing.url
}