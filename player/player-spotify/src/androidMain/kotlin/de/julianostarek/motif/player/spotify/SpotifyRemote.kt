@file:JvmName("SpotifyTopLevel")

package de.julianostarek.motif.player.spotify

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

public fun isSpotifyInstalled(context: Context): Boolean = SpotifyAppRemote.isSpotifyInstalled(context)

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
        .setRedirectUri(redirectUri)
        .showAuthView(true)
        .build()
}

public actual class SpotifyRemoteConnector actual constructor(
    connectionParams: SpotifyRemoteConnectionParams,
    private val externalScope: CoroutineScope
) : Connector.ConnectionListener {
    private val _state: MutableStateFlow<SpotifyRemoteConnectionState> =
        MutableStateFlow(SpotifyRemoteConnectionState.Disconnected())
    public actual val state: StateFlow<SpotifyRemoteConnectionState> get() = _state

    private val connectionParams: ConnectionParams = connectionParams.connectionParams

    override fun onConnected(spotifyAppRemote: SpotifyAppRemote?) {
        val remote = SpotifyRemote(spotifyAppRemote!!)
        _state.value = SpotifyRemoteConnectionState.Connected(remote)
    }

    override fun onFailure(error: Throwable?) {
        _state.value = SpotifyRemoteConnectionState.Disconnected(error)
    }

    public fun connect(context: Context) {
        SpotifyAppRemote.connect(context, connectionParams, this)
    }

    public suspend fun connectAndPlay(context: Context, uri: String) {
        val remote = state.onSubscription { connect(context) }
            .onEach { println(it) }
            .take(2)
            .filterIsInstance<SpotifyRemoteConnectionState.Connected>()
            .firstOrNull()?.remote
        remote?.playerApi?.play(uri)
    }

    public actual fun disconnect() {
        val state = this.state.value
        if (state is SpotifyRemoteConnectionState.Connected) {
            state.remote.android.let(SpotifyAppRemote::disconnect)
        }
        _state.value = SpotifyRemoteConnectionState.Disconnected()
    }
}
