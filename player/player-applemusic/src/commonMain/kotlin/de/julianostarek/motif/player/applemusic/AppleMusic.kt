package de.julianostarek.motif.player.applemusic

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

public expect class MusicPlayerController {
    public suspend fun setQueue(storeIds: List<String>, playWhenReady: Boolean)
    public suspend fun play()
    public suspend fun pause()
    public suspend fun stop()
    public suspend fun release()

    public fun playbackStateChanged(): Flow<PlaybackState>

    public val currentItem: MusicPlayerMediaItem?
    public val playbackState: PlaybackState
    public var playbackPosition: Long
    public var playbackRate: Float
    public var repeatMode: RepeatMode
    public var shuffleMode: ShuffleMode
}

public expect class MusicPlayerMediaItem {
    public val title: String?
    public val albumSubscriptionStoreId: String?
    public val albumTitle: String?
    public val artistSubscriptionStoreId: String?
    public val artistName: String?
    public val albumArtistName: String?
    public val url: String?
    public val genreName: String?
    public val composerName: String?
    public val duration: Long
    public val releaseDate: LocalDate?
    public val albumTrackNumber: Int
    public val albumTrackCount: Int
    public val albumDiscNumber: Int
    public val albumDiscCount: Int
    public val isExplicitContent: Boolean
    public val hasLyricsAvailable: Boolean
    public val lyrics: String?
}

public enum class PlaybackState {
    STOPPED, PAUSED, INTERRUPTED, PLAYING, SEEKING_BACKWARD, SEEKING_FORWARD
}

public enum class RepeatMode {
    OFF, ONE, ALL, DEFAULT
}

public enum class ShuffleMode {
    OFF, SONGS, ALBUMS, DEFAULT
}