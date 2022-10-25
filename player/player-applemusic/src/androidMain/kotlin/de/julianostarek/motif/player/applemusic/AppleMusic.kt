package de.julianostarek.motif.player.applemusic


import com.apple.android.music.playback.controller.MediaPlayerController
import com.apple.android.music.playback.model.MediaItemType
import com.apple.android.music.playback.model.MediaPlayerException
import com.apple.android.music.playback.model.PlaybackRepeatMode
import com.apple.android.music.playback.model.PlaybackShuffleMode
import com.apple.android.music.playback.model.PlayerMediaItem
import com.apple.android.music.playback.model.PlayerQueueItem
import com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider
import com.apple.android.music.playback.queue.PlaybackQueueInsertionType
import com.apple.android.music.playback.queue.PlaybackQueueItemProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime

public actual class MusicPlayerController(
    private val controller: MediaPlayerController,
    externalScope: CoroutineScope,
) {
    private val listener = object : MediaPlayerController.Listener {
        override fun onPlayerStateRestored(p0: MediaPlayerController) {
            // nop
        }

        override fun onPlaybackStateChanged(p0: MediaPlayerController, p1: Int, p2: Int) {
            _playbackStateChanged.tryEmit(p2.playbackStateToCommon())
        }

        override fun onPlaybackStateUpdated(p0: MediaPlayerController) {
            // nop
        }

        override fun onBufferingStateChanged(p0: MediaPlayerController, p1: Boolean) {
            // nop
        }

        override fun onCurrentItemChanged(p0: MediaPlayerController, p1: PlayerQueueItem?, p2: PlayerQueueItem?) {
            p1?.let { _currentItemChanged.tryEmit(MusicPlayerMediaItem(it.item)) }
        }

        override fun onItemEnded(p0: MediaPlayerController, p1: PlayerQueueItem, p2: Long) {
            // nop
        }

        override fun onMetadataUpdated(p0: MediaPlayerController, p1: PlayerQueueItem) {
            // nop
        }

        override fun onPlaybackQueueChanged(p0: MediaPlayerController, p1: MutableList<PlayerQueueItem>) {
            // nop
        }

        override fun onPlaybackQueueItemsAdded(p0: MediaPlayerController, p1: Int, p2: Int, p3: Int) {
            // nop
        }

        override fun onPlaybackError(p0: MediaPlayerController, p1: MediaPlayerException) {
            // nop
        }

        override fun onPlaybackRepeatModeChanged(p0: MediaPlayerController, p1: Int) {
            // nop
        }

        override fun onPlaybackShuffleModeChanged(p0: MediaPlayerController, p1: Int) {
            // nop
        }
    }
    private val _playbackStateChanged: MutableSharedFlow<PlaybackState> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    private val _currentItemChanged: MutableSharedFlow<MusicPlayerMediaItem?> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    public val currentItemChanged: SharedFlow<MusicPlayerMediaItem?> get() = _currentItemChanged

    init {
        externalScope.launch {
            combine(listOf(_playbackStateChanged.subscriptionCount, _currentItemChanged.subscriptionCount)) { it.sum() }
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { subscribed ->
                    if (subscribed) {
                        controller.addListener(listener)
                    } else {
                        controller.removeListener(listener)
                    }
                }
        }
    }

    public actual suspend fun setQueue(storeIds: List<String>, playWhenReady: Boolean) {
        controller.prepare(
            CatalogPlaybackQueueItemProvider.Builder()
                .items(MediaItemType.SONG, *storeIds.toTypedArray())
                .build(),
            PlaybackQueueInsertionType.INSERTION_TYPE_REPLACE,
            playWhenReady
        )
    }

    public actual suspend fun play() {
        controller.play()
    }

    public actual suspend fun pause() {
        controller.pause()
    }

    public actual suspend fun stop() {
        controller.stop()
    }

    public actual suspend fun release() {
        controller.release()
    }

    public actual fun playbackStateChanged(): Flow<PlaybackState> = _playbackStateChanged

    public actual var playbackRate: Float
        get() = controller.playbackRate
        set(_) {} // Unsupported

    public actual var playbackPosition: Long
        get() = controller.currentPosition
        set(value) = controller.seekToPosition(value)

    public actual val currentItem: MusicPlayerMediaItem?
        get() = controller.currentItem?.let { MusicPlayerMediaItem(it.item) }

    public actual val playbackState: PlaybackState
        get() = controller.playbackState.playbackStateToCommon()

    public actual var repeatMode: RepeatMode
        get() = controller.repeatMode.repeatModeToCommon()
        set(value) {
            controller.repeatMode = value.repeatModeToSpecific()
        }

    public actual var shuffleMode: ShuffleMode
        get() = controller.shuffleMode.shuffleModeToCommon()
        set(value) {
            controller.shuffleMode = value.shuffleModeToSpecific()
        }
}

public fun Int.repeatModeToCommon(): RepeatMode = when (this) {
    PlaybackRepeatMode.REPEAT_MODE_OFF -> RepeatMode.OFF
    PlaybackRepeatMode.REPEAT_MODE_ONE -> RepeatMode.ONE
    PlaybackRepeatMode.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> throw IllegalStateException()
}

public fun RepeatMode.repeatModeToSpecific(): Int = when (this) {
    RepeatMode.OFF -> PlaybackRepeatMode.REPEAT_MODE_OFF
    RepeatMode.ONE -> PlaybackRepeatMode.REPEAT_MODE_ONE
    RepeatMode.ALL -> PlaybackRepeatMode.REPEAT_MODE_ALL
    else -> throw IllegalStateException()
}

public fun Int.shuffleModeToCommon(): ShuffleMode = when (this) {
    PlaybackShuffleMode.SHUFFLE_MODE_OFF -> ShuffleMode.OFF
    PlaybackShuffleMode.SHUFFLE_MODE_SONGS -> ShuffleMode.SONGS
    else -> throw IllegalStateException()
}

public fun ShuffleMode.shuffleModeToSpecific(): Int = when (this) {
    ShuffleMode.OFF -> PlaybackShuffleMode.SHUFFLE_MODE_OFF
    ShuffleMode.SONGS -> PlaybackShuffleMode.SHUFFLE_MODE_SONGS
    else -> throw IllegalStateException()
}

public fun Int.playbackStateToCommon(): PlaybackState = when (this) {
    com.apple.android.music.playback.model.PlaybackState.PAUSED -> PlaybackState.PAUSED
    com.apple.android.music.playback.model.PlaybackState.PLAYING -> PlaybackState.PLAYING
    com.apple.android.music.playback.model.PlaybackState.STOPPED -> PlaybackState.STOPPED
    else -> throw IllegalStateException()
}

public actual class MusicPlayerMediaItem(public val android: PlayerMediaItem) {
    public actual val title: String?
        get() = android.title
    public actual val albumSubscriptionStoreId: String?
        get() = android.albumSubscriptionStoreId
    public actual val albumTitle: String?
        get() = android.albumTitle
    public actual val artistSubscriptionStoreId: String?
        get() = android.artistSubscriptionStoreId
    public actual val artistName: String?
        get() = android.artistName
    public actual val albumArtistName: String?
        get() = android.albumArtistName
    public actual val url: String?
        get() = android.artworkUrl
    public actual val genreName: String?
        get() = android.genreName
    public actual val composerName: String?
        get() = android.composerName
    public actual val duration: Long
        get() = android.duration
    public actual val releaseDate: LocalDate?
        get() = android.releaseDate
            ?.toInstant()
            ?.toKotlinInstant()
            ?.toLocalDateTime(TimeZone.UTC)
            ?.date
    public actual val albumTrackNumber: Int
        get() = android.albumTrackNumber
    public actual val albumTrackCount: Int
        get() = android.albumTrackCount
    public actual val albumDiscNumber: Int
        get() = android.albumDiscNumber
    public actual val albumDiscCount: Int
        get() = android.albumDiscCount
    public actual val isExplicitContent: Boolean
        get() = android.isExplicitContent
    public actual val hasLyricsAvailable: Boolean
        get() = android.hasLyricsAvailable()
    public actual val lyrics: String?
        get() = android.a()

    public val artworkUrl: String?
        get() = android.artworkUrl

    public fun artworkUrl(width: Int, height: Int): String? {
        return android.getArtworkUrl(width, height)
    }

    public val isPlayableContent: Boolean
        get() = android.isPlayableContent
}