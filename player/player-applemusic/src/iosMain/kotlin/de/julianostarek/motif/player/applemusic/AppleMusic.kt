package de.julianostarek.motif.player.applemusic

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNotificationCenter
import platform.MediaPlayer.*
import platform.StoreKit.*
import platform.UIKit.UIImage
import platform.darwin.NSObjectProtocol

public fun abc(listener: (UIImage?) -> Unit) {
    SKCloudServiceController.requestAuthorization { status ->
        when (status) {
            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusAuthorized -> {
                println("authorized")
                SKCloudServiceController().requestCapabilitiesWithCompletionHandler { capabilities, nsError ->
                    println(capabilities)
                    println(SKCloudServiceCapabilityMusicCatalogPlayback)
                    when {
                        capabilities and SKCloudServiceCapabilityMusicCatalogPlayback == SKCloudServiceCapabilityMusicCatalogPlayback -> {
                            println("MusicCatalogPlayback")
                        }

                        else -> {
                            println("other/no capabilities")
                        }
                    }
                }
            }

            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusNotDetermined -> {
                println("notDetermined")
            }

            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusRestricted -> {
                println("restricted")
            }

            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusDenied -> {
                println("denied")
            }

            else -> {}
        }
    }

    MPMusicPlayerController.systemMusicPlayer.setQueueWithStoreIDs(listOf("1405193725"))
    MPMusicPlayerController.systemMusicPlayer.play()
    MPMusicPlayerController.systemMusicPlayer.currentPlaybackTime = 120.0
}

public actual class MusicPlayerController(externalScope: CoroutineScope) {
    private val musicPlayer: MPMusicPlayerController get() = MPMusicPlayerController.systemMusicPlayer
    private val _playbackStateChanged: MutableSharedFlow<PlaybackState> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    public val playbackStateChanged: SharedFlow<PlaybackState> get() = _playbackStateChanged
    private val _currentItemChanged: MutableSharedFlow<MusicPlayerMediaItem?> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    public val currentItemChanged: SharedFlow<MusicPlayerMediaItem?> get() = _currentItemChanged

    private var playbackStateObserver: NSObjectProtocol? = null
    private var currentItemObserver: NSObjectProtocol? = null

    init {
        externalScope.launch {
            combine(listOf(_playbackStateChanged.subscriptionCount, _currentItemChanged.subscriptionCount)) { it.sum() }
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { subscribed ->
                    if (subscribed) {
                        subscribeToUpdates()
                    } else {
                        unsubscribeFromUpdates()
                    }
                }
        }
    }

    private fun subscribeToUpdates() {
        println("subscribe")
        playbackStateObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            MPMusicPlayerControllerPlaybackStateDidChangeNotification,
            MPMusicPlayerController.systemMusicPlayer,
            queue = null
        ) { notification ->
            _playbackStateChanged.tryEmit(musicPlayer.playbackState.toCommon())
        }
        currentItemObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            MPMusicPlayerControllerNowPlayingItemDidChangeNotification,
            MPMusicPlayerController.systemMusicPlayer,
            queue = null
        ) { notification ->
            _currentItemChanged.tryEmit(musicPlayer.nowPlayingItem?.let(::MusicPlayerMediaItem))
        }
        musicPlayer.beginGeneratingPlaybackNotifications()
    }

    private fun unsubscribeFromUpdates() {
        println("unsubscribe")
        musicPlayer.endGeneratingPlaybackNotifications()
        playbackStateObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        currentItemObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
    }

    public actual suspend fun play() {
        musicPlayer.play()
    }

    public actual suspend fun pause() {
        musicPlayer.pause()
    }

    public actual suspend fun stop() {
        musicPlayer.stop()
    }

    public actual var playbackRate: Float
        get() = musicPlayer.currentPlaybackRate
        set(value) = musicPlayer.setCurrentPlaybackRate(value)

    public actual var playbackPosition: Long
        get() = (musicPlayer.currentPlaybackTime * 1000).toLong()
        set(value) = musicPlayer.setCurrentPlaybackTime(value / 1000.0)

    public actual val currentItem: MusicPlayerMediaItem?
        get() = musicPlayer.nowPlayingItem?.let(::MusicPlayerMediaItem)

    public actual val playbackState: PlaybackState
        get() = musicPlayer.playbackState.toCommon()

    public actual var repeatMode: RepeatMode
        get() = musicPlayer.repeatMode.toCommon()
        set(value) {
            musicPlayer.repeatMode = value.toSpecific()
        }

    public actual var shuffleMode: ShuffleMode
        get() = musicPlayer.shuffleMode.toCommon()
        set(value) {
            musicPlayer.shuffleMode = value.toSpecific()
        }
}

public fun MPMusicRepeatMode.toCommon(): RepeatMode = when (this) {
    MPMusicRepeatMode.MPMusicRepeatModeNone -> RepeatMode.OFF
    MPMusicRepeatMode.MPMusicRepeatModeOne -> RepeatMode.ONE
    MPMusicRepeatMode.MPMusicRepeatModeDefault -> RepeatMode.DEFAULT
    MPMusicRepeatMode.MPMusicRepeatModeAll -> RepeatMode.ALL
    else -> throw IllegalStateException()
}

public fun RepeatMode.toSpecific(): MPMusicRepeatMode = when (this) {
    RepeatMode.OFF -> MPMusicRepeatMode.MPMusicRepeatModeNone
    RepeatMode.ONE -> MPMusicRepeatMode.MPMusicRepeatModeOne
    RepeatMode.DEFAULT -> MPMusicRepeatMode.MPMusicRepeatModeDefault
    RepeatMode.ALL -> MPMusicRepeatMode.MPMusicRepeatModeAll
    else -> throw IllegalStateException()
}

public fun MPMusicShuffleMode.toCommon(): ShuffleMode = when (this) {
    MPMusicShuffleMode.MPMusicShuffleModeSongs -> ShuffleMode.SONGS
    MPMusicShuffleMode.MPMusicShuffleModeOff -> ShuffleMode.OFF
    MPMusicShuffleMode.MPMusicShuffleModeAlbums -> ShuffleMode.ALBUMS
    MPMusicShuffleMode.MPMusicShuffleModeDefault -> ShuffleMode.DEFAULT
    else -> throw IllegalStateException()
}

public fun ShuffleMode.toSpecific(): MPMusicShuffleMode = when (this) {
    ShuffleMode.SONGS -> MPMusicShuffleMode.MPMusicShuffleModeSongs
    ShuffleMode.OFF -> MPMusicShuffleMode.MPMusicShuffleModeOff
    ShuffleMode.ALBUMS -> MPMusicShuffleMode.MPMusicShuffleModeAlbums
    ShuffleMode.DEFAULT -> MPMusicShuffleMode.MPMusicShuffleModeDefault
    else -> throw IllegalStateException()
}

public fun MPMusicPlaybackState.toCommon(): PlaybackState = when (this) {
    MPMusicPlaybackState.MPMusicPlaybackStateSeekingBackward -> PlaybackState.SEEKING_BACKWARD
    MPMusicPlaybackState.MPMusicPlaybackStateSeekingForward -> PlaybackState.SEEKING_FORWARD
    MPMusicPlaybackState.MPMusicPlaybackStatePlaying -> PlaybackState.PLAYING
    MPMusicPlaybackState.MPMusicPlaybackStateStopped -> PlaybackState.STOPPED
    MPMusicPlaybackState.MPMusicPlaybackStatePaused,
    MPMusicPlaybackState.MPMusicPlaybackStateInterrupted -> PlaybackState.PAUSED

    else -> throw IllegalStateException()
}

public actual class MusicPlayerMediaItem(public val ios: MPMediaItem) {
    public actual val title: String?
        get() = ios.title
    public actual val albumSubscriptionStoreId: String?
        get() = ios.albumPersistentID.toString()
    public actual val albumTitle: String?
        get() = ios.albumTitle
    public actual val artistSubscriptionStoreId: String?
        get() = ios.artistPersistentID.toString()
    public actual val artistName: String?
        get() = ios.artist
    public actual val albumArtistName: String?
        get() = ios.albumArtist
    public actual val url: String?
        get() = ios.assetURL?.absoluteString
    public actual val genreName: String?
        get() = ios.genre
    public actual val composerName: String?
        get() = ios.composer
    public actual val duration: Long
        get() = (ios.playbackDuration * 1000).toLong()
    public actual val releaseDate: LocalDate?
        get() = ios.releaseDate?.toKotlinInstant()?.toLocalDateTime(TimeZone.UTC)?.date
    public actual val albumTrackNumber: Int
        get() = ios.albumTrackNumber.toInt()
    public actual val albumTrackCount: Int
        get() = ios.albumTrackCount.toInt()
    public actual val albumDiscNumber: Int
        get() = ios.discNumber.toInt()
    public actual val albumDiscCount: Int
        get() = ios.discCount.toInt()
    public actual val isExplicitContent: Boolean
        get() = ios.explicitItem
    public actual val hasLyricsAvailable: Boolean
        get() = ios.lyrics != null
    public actual val lyrics: String?
        get() = ios.lyrics

    public fun artwork(width: Int, height: Int): UIImage? {
        return ios.artwork?.imageWithSize(CGSizeMake(width.toDouble(), height.toDouble()))
    }

    public fun artwork(): UIImage? {
        return ios.artwork?.let { artwork ->
            artwork.imageWithSize(artwork.bounds.useContents { size }.readValue())
        }
    }
}