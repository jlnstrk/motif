package de.julianostarek.motif.player.spotify

import android.graphics.Bitmap
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.client.Subscription.LifecycleCallback
import com.spotify.protocol.types.Capabilities
import com.spotify.protocol.types.Image
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.Repeat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public suspend fun <T> CallResult<T>.suspendAwait(): T {
    return suspendCancellableCoroutine { continuation ->
        setResultCallback(continuation::resume)
        setErrorCallback(continuation::resumeWithException)
        continuation.invokeOnCancellation { cancel() }
    }
}

public fun <T> Subscription<T>.asFlow(): Flow<T> = callbackFlow {
    this@asFlow.setEventCallback { trySendBlocking(it) }
    this@asFlow.setErrorCallback { throw it }
    this@asFlow.setLifecycleCallback(object : LifecycleCallback {
        override fun onStart() {
            // nop
        }

        override fun onStop() {
            channel.close()
        }
    })
    awaitClose { this@asFlow.cancel() }
}

public actual class Track(public val android: com.spotify.protocol.types.Track) {
    public actual val artist: Artist get() = android.artist
    public actual val artists: List<Artist>
        get() = android.artists
    public actual val album: Album
        get() = android.album
    public actual val duration: Long
        get() = android.duration
    public actual val name: String
        get() = android.name
    public actual val uri: String
        get() = android.uri
    public actual val imageUri: String?
        get() = android.imageUri.raw
    public actual val isEpisode: Boolean
        get() = android.isEpisode
    public actual val isPodcast: Boolean
        get() = android.isPodcast
}

public actual typealias Album = com.spotify.protocol.types.Album

public actual typealias Artist = com.spotify.protocol.types.Artist

public actual class PlayerState(public val android: com.spotify.protocol.types.PlayerState) {
    public actual val track: Track?
        get() = android.track?.let { androidTrack ->
            // Track properties may be null
            if (androidTrack.name != null) Track(androidTrack) else null
        }
    public actual val isPaused: Boolean
        get() = android.isPaused
    public actual val playbackSpeed: Float
        get() = android.playbackSpeed
    public actual val playbackPosition: Long
        get() = android.playbackPosition
    public actual val playbackOptions: PlayerOptions?
        get() = android.playbackOptions?.let(::PlayerOptions)
    public actual val playbackRestrictions: PlayerRestrictions?
        get() = android.playbackRestrictions
}

public actual class PlayerOptions(public val android: com.spotify.protocol.types.PlayerOptions) {
    public actual val isShuffling: Boolean
        get() = android.isShuffling
    public actual val repeatMode: RepeatMode
        get() = android.repeatMode.repeatModeToCommon()
}

public fun Int.repeatModeToCommon(): RepeatMode = when (this) {
    Repeat.OFF -> RepeatMode.OFF
    Repeat.ONE -> RepeatMode.TRACK
    Repeat.ALL -> RepeatMode.CONTEXT
    else -> throw IllegalStateException()
}

public fun RepeatMode.repeatModeToSpecific(): Int = when (this) {
    RepeatMode.OFF -> Repeat.OFF
    RepeatMode.TRACK -> Repeat.ONE
    RepeatMode.CONTEXT -> Repeat.ALL
}

public actual typealias PlayerRestrictions = com.spotify.protocol.types.PlayerRestrictions

public typealias PlayerContext = com.spotify.protocol.types.PlayerContext

public actual class CrossfadeState(public val android: com.spotify.protocol.types.CrossfadeState) {
    public actual val duration: Long
        get() = android.duration.toLong()
    public actual val enabled: Boolean
        get() = android.isEnabled
}

public actual class PlayerApi(public val android: com.spotify.android.appremote.api.PlayerApi) {
    public actual fun playerState(): Flow<PlayerState> =
        android.subscribeToPlayerState().asFlow().map(::PlayerState)

    public actual suspend fun play(uri: String) {
        android.play(uri).suspendAwait()
    }

    public actual suspend fun queue(uri: String) {
        android.queue(uri).suspendAwait()
    }

    public actual suspend fun resume() {
        android.resume().suspendAwait()
    }

    public actual suspend fun pause() {
        android.pause().suspendAwait()
    }

    public actual suspend fun skipNext() {
        android.skipNext().suspendAwait()
    }

    public actual suspend fun skipPrevious() {
        android.skipPrevious().suspendAwait()
    }

    public actual suspend fun setShuffle(enabled: Boolean) {
        android.setShuffle(enabled).suspendAwait()
    }

    public actual suspend fun setRepeat(repeatMode: RepeatMode) {
        android.setRepeat(repeatMode.repeatModeToSpecific()).suspendAwait()
    }

    public actual suspend fun seekTo(positionMs: Long) {
        android.seekTo(positionMs).suspendAwait()
    }

    public actual suspend fun getPlayerState(): PlayerState = android.playerState.suspendAwait().let(::PlayerState)

    public fun subscribeToPlayerContext(): Flow<PlayerContext?> = android.subscribeToPlayerContext().asFlow()

    public actual suspend fun getCrossfadeState(): CrossfadeState? =
        android.crossfadeState.suspendAwait()?.let(::CrossfadeState)
}

public actual class ImagesApi(public val android: com.spotify.android.appremote.api.ImagesApi) {
    public suspend fun getImage(track: Track): Bitmap {
        return android.getImage(ImageUri(track.imageUri)).suspendAwait()
    }

    public suspend fun getImage(track: Track, dimension: ImageDimension): Bitmap {
        return android.getImage(
            ImageUri(track.imageUri), when (dimension) {
                ImageDimension.LARGE -> Image.Dimension.LARGE
                ImageDimension.MEDIUM -> Image.Dimension.MEDIUM
                ImageDimension.SMALL -> Image.Dimension.SMALL
                ImageDimension.X_SMALL -> Image.Dimension.X_SMALL
                ImageDimension.THUMBNAIL -> Image.Dimension.THUMBNAIL
            }
        ).suspendAwait()
    }
}

public actual class UserApi(
    public val android: com.spotify.android.appremote.api.UserApi
) {
    public actual fun userCapabilities(): Flow<UserCapabilities> =
        android.subscribeToCapabilities().asFlow().map(::UserCapabilities)

    public actual suspend fun getUserCapabilities(): UserCapabilities {
        val capabilities = android.capabilities.suspendAwait()
        return UserCapabilities(capabilities)
    }
}

public actual class UserCapabilities(public val android: Capabilities) {
    public actual val canPlayOnDemand: Boolean get() = android.canPlayOnDemand
}