package de.julianostarek.motif.player.spotify

import cocoapods.SpotifyiOS.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.native.internal.ObjCErrorException

public fun isSpotifyInstalled(): Boolean {
    val url = NSURL(string = "spotify://spotify")
    return UIApplication.sharedApplication.canOpenURL(url)
    // TODO: Doesn't work. Why?
    // return SPTSessionManager().isSpotifyAppInstalled()
}

public actual class SpotifyRemote(
    public val ios: SPTAppRemote,
    externalScope: CoroutineScope
) {
    public actual val isConnected: Boolean
        get() = ios.isConnected()
    public actual val playerApi: PlayerApi by lazy { PlayerApi(ios.playerAPI!!, externalScope) }
    public actual val imagesApi: ImagesApi by lazy { ImagesApi(ios.imageAPI!!) }
    public actual val userApi: UserApi by lazy { UserApi(ios.userAPI!!, externalScope) }
}

public actual class SpotifyRemoteConnectionParams actual constructor(clientId: String, redirectUri: String) {
    public val configuration: SPTConfiguration = SPTConfiguration(clientId, NSURL(string = redirectUri))
}

@Suppress("CONFLICTING_OVERLOADS")
public actual class SpotifyRemoteConnector actual constructor(
    connectionParams: SpotifyRemoteConnectionParams,
    externalScope: CoroutineScope
) {
    private val _state: MutableStateFlow<SpotifyRemoteConnectionState> =
        MutableStateFlow(SpotifyRemoteConnectionState.Disconnected())
    public actual val state: StateFlow<SpotifyRemoteConnectionState> get() = _state

    private val appRemote: SPTAppRemote = SPTAppRemote(connectionParams.configuration, SPTAppRemoteLogLevelDebug)

    private val delegate = object : NSObject(), SPTAppRemoteDelegateProtocol {
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun appRemote(appRemote: SPTAppRemote, didDisconnectWithError: NSError?) {
            _state.value = SpotifyRemoteConnectionState.Disconnected(didDisconnectWithError)
        }

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun appRemote(appRemote: SPTAppRemote, didFailConnectionAttemptWithError: NSError?) {
            _state.value = SpotifyRemoteConnectionState.FailedToConnect(didFailConnectionAttemptWithError)
        }

        override fun appRemoteDidEstablishConnection(appRemote: SPTAppRemote) {
            val remote = SpotifyRemote(appRemote, externalScope)
            _state.value = SpotifyRemoteConnectionState.Connected(remote)
        }
    }

    init {
        appRemote.setDelegate(delegate)
    }

    public fun connect() {
        appRemote.authorizeAndPlayURI("")
    }

    public fun connectAndPlay(uri: String) {
        appRemote.authorizeAndPlayURI(uri)
    }

    public fun authorizeFromUrl(url: NSURL) {
        println("authorizeFromUrl: ${url.absoluteString()}")
        val parameters = appRemote.authorizationParametersFromURL(url) ?: return
        val accessToken = parameters[SPTAppRemoteAccessTokenKey]
        (accessToken as? String)?.let {
            appRemote.connectionParameters.setAccessToken(accessToken)
            appRemote.connect()
        }
    }

    public actual fun disconnect() {
        appRemote.disconnect()
        _state.value = SpotifyRemoteConnectionState.Disconnected()
    }
}

public suspend inline fun <reified T> suspendAwaitRemoteCallback(
    crossinline register: (SPTAppRemoteCallback) -> Unit
): T = suspendCoroutine { continuation ->
    val callback = object : SPTAppRemoteCallback {
        override fun invoke(p1: Any?, p2: NSError?) {
            if (p1 != null) {
                continuation.resume(p1 as T)
            } else {
                continuation.resumeWithException(ObjCErrorException(p2!!.localizedDescription, p2))
            }
        }
    }
    register(callback)
}