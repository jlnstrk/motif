package de.julianostarek.motif.player.applemusic

import de.julianostarek.motif.player.Player
import de.julianostarek.motif.player.PlayerState
import kotlin.jvm.JvmInline

@JvmInline
public value class AppleMusicPlayer(internal val backing: MusicPlayerController) : Player {
    override suspend fun resume() {
        backing.play()
    }

    override suspend fun pause() {
        backing.pause()
    }

    override suspend fun stop() {
        backing.stop()
    }

    override suspend fun playerState(): PlayerState {
        return AppleMusicPlayerState(backing)
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