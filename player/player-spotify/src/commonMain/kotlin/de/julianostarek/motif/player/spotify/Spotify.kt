package de.julianostarek.motif.player.spotify

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

public expect class Track {
    public val artist: Artist
    public val artists: List<Artist>
    public val album: Album
    public val duration: Long
    public val name: String
    public val uri: String
    public val imageUri: String?
    public val isEpisode: Boolean
    public val isPodcast: Boolean
}

public expect class Album {
    public val name: String
    public val uri: String
}

public expect class Artist {
    public val name: String
    public val uri: String
}

public expect class PlayerOptions {
    public val isShuffling: Boolean
    public val repeatMode: RepeatMode
}

public enum class RepeatMode {
    OFF, TRACK, CONTEXT
}

public expect class PlayerRestrictions {
    public val canSkipNext: Boolean
    public val canSkipPrev: Boolean
    public val canRepeatTrack: Boolean
    public val canRepeatContext: Boolean
    public val canToggleShuffle: Boolean
    public val canSeek: Boolean
}

public expect class PlayerState {
    public val track: Track?
    public val isPaused: Boolean
    public val playbackSpeed: Float
    public val playbackPosition: Long
    public val playbackOptions: PlayerOptions?
    public val playbackRestrictions: PlayerRestrictions?
}

public expect class CrossfadeState {
    public val duration: Long
    public val enabled: Boolean
}

public expect class PlayerApi {
    public fun playerState(): Flow<PlayerState>
    public suspend fun play(uri: String)
    public suspend fun queue(uri: String)
    public suspend fun resume()
    public suspend fun pause()
    public suspend fun skipNext()
    public suspend fun skipPrevious()
    public suspend fun setShuffle(enabled: Boolean)
    public suspend fun setRepeat(repeatMode: RepeatMode)
    public suspend fun seekTo(positionMs: Long)
    public suspend fun getPlayerState(): PlayerState
    public suspend fun getCrossfadeState(): CrossfadeState?
}

public expect class ImagesApi

public expect class UserApi {
    public fun userCapabilities(): Flow<UserCapabilities>
    public suspend fun getUserCapabilities(): UserCapabilities
}

public expect class UserCapabilities {
    public val canPlayOnDemand: Boolean
}

public enum class ImageDimension {
    LARGE, MEDIUM, SMALL, X_SMALL, THUMBNAIL
}

