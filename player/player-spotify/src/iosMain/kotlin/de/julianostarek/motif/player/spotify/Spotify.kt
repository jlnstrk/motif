package de.julianostarek.motif.player.spotify

import cocoapods.SpotifyiOS.*
import kotlinx.cinterop.CValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIImage
import platform.darwin.NSObject

public actual class Artist(public val ios: SPTAppRemoteArtistProtocol) {
    public actual val name: String
        get() = ios.name()
    public actual val uri: String
        get() = ios.URI()
}

public actual class Album(public val ios: SPTAppRemoteAlbumProtocol) {
    public actual val name: String
        get() = ios.name()
    public actual val uri: String
        get() = ios.URI()
}

public actual class Track(public val ios: SPTAppRemoteTrackProtocol) {
    public actual val artist: Artist
        get() = Artist(ios.artist())
    public actual val artists: List<Artist>
        get() = listOf(artist)
    public actual val album: Album
        get() = Album(ios.album())
    public actual val duration: Long
        get() = ios.duration().toLong()
    public actual val name: String
        get() = ios.name()
    public actual val uri: String
        get() = ios.URI()
    public actual val imageUri: String?
        get() = ios.imageIdentifier()
    public actual val isEpisode: Boolean
        get() = ios.isEpisode()
    public actual val isPodcast: Boolean
        get() = ios.isPodcast()
}

public actual class PlayerState(public val ios: SPTAppRemotePlayerStateProtocol) {
    public actual val track: Track?
        get() = Track(ios.track())
    public actual val isPaused: Boolean
        get() = ios.isPaused()
    public actual val playbackSpeed: Float
        get() = ios.playbackSpeed()
    public actual val playbackPosition: Long
        get() = ios.playbackPosition()
    public actual val playbackOptions: PlayerOptions?
        get() = PlayerOptions(ios.playbackOptions())
    public actual val playbackRestrictions: PlayerRestrictions?
        get() = PlayerRestrictions(ios.playbackRestrictions())
}

public actual class PlayerOptions(public val ios: SPTAppRemotePlaybackOptionsProtocol) {
    public actual val isShuffling: Boolean
        get() = ios.isShuffling()
    public actual val repeatMode: RepeatMode
        get() = ios.repeatMode().toCommon()
}

public fun SPTAppRemotePlaybackOptionsRepeatMode.toCommon(): RepeatMode = when (this) {
    SPTAppRemotePlaybackOptionsRepeatModeOff -> RepeatMode.OFF
    SPTAppRemotePlaybackOptionsRepeatModeTrack -> RepeatMode.TRACK
    SPTAppRemotePlaybackOptionsRepeatModeContext -> RepeatMode.CONTEXT
    else -> throw IllegalStateException()
}

public fun RepeatMode.toSpecific(): SPTAppRemotePlaybackOptionsRepeatMode = when (this) {
    RepeatMode.OFF -> SPTAppRemotePlaybackOptionsRepeatModeOff
    RepeatMode.TRACK -> SPTAppRemotePlaybackOptionsRepeatModeTrack
    RepeatMode.CONTEXT -> SPTAppRemotePlaybackOptionsRepeatModeContext
}

public actual class PlayerRestrictions(public val ios: SPTAppRemotePlaybackRestrictionsProtocol) {
    public actual val canSkipNext: Boolean
        get() = ios.canSkipNext()
    public actual val canSkipPrev: Boolean
        get() = ios.canSkipPrevious()
    public actual val canRepeatTrack: Boolean
        get() = ios.canRepeatTrack()
    public actual val canRepeatContext: Boolean
        get() = ios.canRepeatContext()
    public actual val canToggleShuffle: Boolean
        get() = ios.canToggleShuffle()
    public actual val canSeek: Boolean
        get() = ios.canSeek()
}

public actual class CrossfadeState(public val ios: SPTAppRemoteCrossfadeStateProtocol) {
    public actual val duration: Long
        get() = ios.duration()
    public actual val enabled: Boolean
        get() = ios.isEnabled()
}

public actual class PlayerApi(
    public val ios: SPTAppRemotePlayerAPIProtocol,
    externalScope: CoroutineScope
) {

    private val _playerState: MutableSharedFlow<PlayerState> = MutableSharedFlow()
    private val delegate = object : NSObject(), SPTAppRemotePlayerStateDelegateProtocol {
        override fun playerStateDidChange(playerState: SPTAppRemotePlayerStateProtocol) {
            externalScope.launch {
                _playerState.emit(PlayerState(playerState))
            }
        }
    }

    public actual fun playerState(): Flow<PlayerState> = _playerState

    init {
        ios.delegate = delegate
        externalScope.launch {
            _playerState.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { hasSubscribers ->
                    if (hasSubscribers) {
                        if (!suspendAwaitRemoteCallback<Boolean>(ios::subscribeToPlayerState)) {
                            println("PlayerApi: Failed to subscribe to player state")
                        }
                    } else {
                        if (!suspendAwaitRemoteCallback<Boolean>(ios::unsubscribeToPlayerState)) {
                            println("PlayerApi: Failed to unsubscribe from player state")
                        }
                    }
                }
        }
    }

    public actual suspend fun play(uri: String) {
        suspendAwaitRemoteCallback<Any> { ios.play(uri, it) }
    }

    public actual suspend fun queue(uri: String) {
        suspendAwaitRemoteCallback<Any> { ios.enqueueTrackUri(uri, it) }
    }

    public actual suspend fun resume() {
        suspendAwaitRemoteCallback<Any>(ios::resume)
    }

    public actual suspend fun pause() {
        suspendAwaitRemoteCallback<Any>(ios::pause)
    }

    public actual suspend fun skipNext() {
        suspendAwaitRemoteCallback<Any>(ios::skipToNext)
    }

    public actual suspend fun skipPrevious() {
        suspendAwaitRemoteCallback<Any>(ios::skipToPrevious)
    }

    public actual suspend fun setShuffle(enabled: Boolean) {
        suspendAwaitRemoteCallback<Any> { ios.setShuffle(enabled, it) }
    }

    public actual suspend fun setRepeat(repeatMode: RepeatMode) {
        suspendAwaitRemoteCallback<Any> { ios.setRepeatMode(repeatMode.toSpecific(), it) }
    }

    public actual suspend fun seekTo(positionMs: Long) {
        suspendAwaitRemoteCallback<Any> { ios.seekToPosition(positionMs, it) }
    }

    public actual suspend fun getPlayerState(): PlayerState {
        val iosPlayerState = suspendAwaitRemoteCallback<SPTAppRemotePlayerStateProtocol>(ios::getPlayerState)
        return PlayerState(iosPlayerState)
    }

    public actual suspend fun getCrossfadeState(): CrossfadeState? {
        val iosCrossfadeState = suspendAwaitRemoteCallback<SPTAppRemoteCrossfadeStateProtocol>(ios::getCrossfadeState)
        return CrossfadeState(iosCrossfadeState)
    }
}

public actual class ImagesApi(public val ios: SPTAppRemoteImageAPIProtocol) {
    public suspend fun getImage(track: Track, width: Int, height: Int): UIImage? {
        println("ImagesApi: getImage(): width: $width, height: $height, track: ${track.uri}")
        if (track.ios.imageIdentifier().isEmpty()) return null
        return suspendAwaitRemoteCallback {
            @Suppress("UNCHECKED_CAST")
            ios.fetchImageForItem(
                track.ios,
                CGSizeMake(width.toDouble(), height.toDouble()) as CValue<CGSize>,
                it
            )
        }
    }
}

public actual class UserApi(
    public val ios: SPTAppRemoteUserAPIProtocol,
    externalScope: CoroutineScope
) {
    private val delegate = object : NSObject(), SPTAppRemoteUserAPIDelegateProtocol {
        override fun userAPI(
            userAPI: SPTAppRemoteUserAPIProtocol,
            didReceiveCapabilities: SPTAppRemoteUserCapabilitiesProtocol
        ) {
            externalScope.launch {
                _userCapabilities.emit(UserCapabilities(didReceiveCapabilities))
            }
        }
    }
    private val _userCapabilities: MutableSharedFlow<UserCapabilities> = MutableSharedFlow()
    public actual fun userCapabilities(): Flow<UserCapabilities> = _userCapabilities

    init {
        ios.delegate = delegate
        externalScope.launch {
            _userCapabilities.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { hasSubscribers ->
                    if (hasSubscribers) {
                        if (!suspendAwaitRemoteCallback<Boolean>(ios::subscribeToCapabilityChanges)) {
                            println("UserApi: Failed to subscribe to capability changes")
                        }
                    } else {
                        if (!suspendAwaitRemoteCallback<Boolean>(ios::unsubscribeToCapabilityChanges)) {
                            println("UserApi: Failed to unsubscribe from capability changes")
                        }
                    }
                }
        }
    }

    public actual suspend fun getUserCapabilities(): UserCapabilities {
        val capabilities =
            suspendAwaitRemoteCallback<SPTAppRemoteUserCapabilitiesProtocol>(ios::fetchCapabilitiesWithCallback)
        return UserCapabilities(capabilities)
    }
}

public actual class UserCapabilities(public val ios: SPTAppRemoteUserCapabilitiesProtocol) {
    public actual val canPlayOnDemand: Boolean get() = ios.canPlayOnDemand
}