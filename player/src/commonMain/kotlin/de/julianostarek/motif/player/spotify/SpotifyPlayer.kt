package de.julianostarek.motif.player.spotify

import de.julianostarek.motif.player.Player
import de.julianostarek.motif.player.PlayerState
import de.julianostarek.motif.player.applemusic.MusicPlayerMediaItem
import de.julianostarek.motif.player.applemusic.PlaybackState
import de.julianostarek.motif.player.applemusic.RepeatMode
import de.julianostarek.motif.player.applemusic.ShuffleMode
import kotlin.jvm.JvmInline

@JvmInline
public value class SpotifyPlayer(internal val backing: PlayerApi) : Player {
    override suspend fun resume() {
        backing.resume()
    }

    override suspend fun pause() {
        backing.pause()
    }

    override suspend fun stop() {
        // nop
    }

    override suspend fun playerState(): PlayerState {
        return SpotifyPlayerState(backing.getPlayerState())
    }

    override suspend fun seekTo(position: Long) {
        backing.seekTo(position)
    }

    override suspend fun setRepeatMode(repeatMode: PlayerState.RepeatMode) {
        backing.setRepeat(when (repeatMode) {
            PlayerState.RepeatMode.OFF -> de.julianostarek.motif.player.spotify.RepeatMode.OFF
            PlayerState.RepeatMode.ONE -> de.julianostarek.motif.player.spotify.RepeatMode.TRACK
            PlayerState.RepeatMode.ALL -> de.julianostarek.motif.player.spotify.RepeatMode.CONTEXT
        })
    }

    override suspend fun setShuffleMode(shuffleMode: PlayerState.ShuffleMode) {
        backing.setShuffle(shuffleMode == PlayerState.ShuffleMode.ON)
    }
}