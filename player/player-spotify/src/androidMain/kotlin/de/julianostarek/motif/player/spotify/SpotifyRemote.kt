package de.julianostarek.motif.player.spotify

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

public actual class SpotifyRemote(
    public val android: SpotifyAppRemote
) {
    public actual val isConnected: Boolean
        get() = android.isConnected
    public actual val playerApi: PlayerApi by lazy { PlayerApi(android.playerApi) }
    public actual val imagesApi: ImagesApi by lazy { ImagesApi(android.imagesApi) }
    public actual val userApi: UserApi by lazy { UserApi(android.userApi) }
}

public actual class SpotifyRemoteConnectionParams actual constructor(clientId: String, redirectUri: String) {
    public val connectionParams: ConnectionParams = ConnectionParams.Builder(clientId)
        .setAuthMethod(ConnectionParams.AuthMethod.APP_ID)
        .setRedirectUri(redirectUri)
        .showAuthView(true)
        .build()
}

public actual class SpotifyRemoteConnector actual constructor(
    connectionParams: SpotifyRemoteConnectionParams,
    private val externalScope: CoroutineScope
) : Connector.ConnectionListener {
    private val _remote: MutableStateFlow<SpotifyRemote?> = MutableStateFlow(null)
    public actual val remote: StateFlow<SpotifyRemote?> get() = _remote

    public var connectionParams: ConnectionParams = connectionParams.connectionParams

    override fun onConnected(spotifyAppRemote: SpotifyAppRemote?) {
        println("onConnected")
        _remote.value = SpotifyRemote(spotifyAppRemote!!)
    }

    override fun onFailure(error: Throwable?) {
        error?.printStackTrace()
        _remote.value = null
    }

    public fun isSpotifyInstalled(context: Context): Boolean = SpotifyAppRemote.isSpotifyInstalled(context)

    public fun connect(context: Context) {
        SpotifyAppRemote.connect(context, connectionParams, this)
    }

    public suspend fun connectAndPlay(context: Context, uri: String) {
        val remote = remote.onSubscription { connect(context) }
            .onEach { println(it) }
            .take(2)
            .firstOrNull { it != null }
        remote?.playerApi?.play(uri)
    }

    public actual fun disconnect() {
        _remote.value?.let {
            SpotifyAppRemote.disconnect(it.android)
        }
    }
}
