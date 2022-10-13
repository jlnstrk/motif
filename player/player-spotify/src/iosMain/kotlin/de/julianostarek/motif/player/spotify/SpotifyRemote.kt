package de.julianostarek.motif.player.spotify

import cocoapods.SpotifyiOS.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.native.internal.ObjCErrorException

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
    private val _remote: MutableStateFlow<SpotifyRemote?> = MutableStateFlow(null)
    public actual val remote: StateFlow<SpotifyRemote?> get() = _remote

    private val sessionManager: SPTSessionManager = SPTSessionManager()
    private val appRemote: SPTAppRemote = SPTAppRemote(connectionParams.configuration, SPTAppRemoteLogLevelDebug)

    private val delegate = object : NSObject(), SPTAppRemoteDelegateProtocol {
        override fun appRemote(appRemote: SPTAppRemote, didDisconnectWithError: NSError?) {
            _remote.value = null
        }

        override fun appRemote(appRemote: SPTAppRemote, didFailConnectionAttemptWithError: NSError?) {
            _remote.value = null
        }

        override fun appRemoteDidEstablishConnection(appRemote: SPTAppRemote) {
            _remote.value = SpotifyRemote(appRemote, externalScope)
        }
    }

    init {
        appRemote.setDelegate(delegate)
    }

    public fun isSpotifyInstalled(): Boolean = sessionManager.isSpotifyAppInstalled()

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