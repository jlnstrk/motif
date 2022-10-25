package de.julianostarek.motif.player

import de.julianostarek.motif.player.spotify.Artist
import de.julianostarek.motif.player.spotify.RepeatMode
import de.julianostarek.motif.player.spotify.SpotifyRemote
import de.julianostarek.motif.player.spotify.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.jvm.JvmInline

@JvmInline
public value class SpotifyPlayer(internal val backing: SpotifyRemote) : Player {
    override suspend fun resume() {
        backing.playerApi.resume()
    }

    override suspend fun pause() {
        backing.playerApi.pause()
    }

    override suspend fun stop() {
        // nop
    }

    override suspend fun getPlayerState(): PlayerState {
        return SpotifyPlayerState(backing.playerApi.getPlayerState())
    }

    override suspend fun playerState(): Flow<PlayerState> {
        return backing.playerApi.playerState()
            .map(::SpotifyPlayerState)
    }

    override suspend fun seekTo(position: Long) {
        backing.playerApi.seekTo(position)
    }

    override suspend fun setRepeatMode(repeatMode: PlayerState.RepeatMode) {
        backing.playerApi.setRepeat(
            when (repeatMode) {
                PlayerState.RepeatMode.OFF -> de.julianostarek.motif.player.spotify.RepeatMode.OFF
                PlayerState.RepeatMode.ONE -> de.julianostarek.motif.player.spotify.RepeatMode.TRACK
                PlayerState.RepeatMode.ALL -> de.julianostarek.motif.player.spotify.RepeatMode.CONTEXT
            }
        )
    }

    override suspend fun setShuffleMode(shuffleMode: PlayerState.ShuffleMode) {
        backing.playerApi.setShuffle(shuffleMode == PlayerState.ShuffleMode.ON)
    }
}

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