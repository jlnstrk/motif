package de.julianostarek.motif.player.spotify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

public interface SpotifyRemoteConnectionCallback {
    public fun onSpotifyRemoteFailedToConnect(error: Any?)
    public fun onSpotifyRemoteConnected(remote: SpotifyRemote)
    public fun onSpotifyRemoteDisconnected(error: Any?)
}

public expect class SpotifyRemoteConnector(
    connectionParams: SpotifyRemoteConnectionParams,
    externalScope: CoroutineScope
) {
    public val state: StateFlow<SpotifyRemoteConnectionState>

    public fun disconnect()
}

public sealed interface SpotifyRemoteConnectionState {
    public data class FailedToConnect(public val error: Any? = null) : SpotifyRemoteConnectionState
    public data class Disconnected(public val error: Any? = null) : SpotifyRemoteConnectionState
    public data class Connected(public val remote: SpotifyRemote) : SpotifyRemoteConnectionState
}

public fun SpotifyRemoteConnector.remoteOrNull(): SpotifyRemote? =
    (state.value as? SpotifyRemoteConnectionState.Connected)?.remote

public expect class SpotifyRemote {
    public val isConnected: Boolean
    public val playerApi: PlayerApi
    public val imagesApi: ImagesApi
    public val userApi: UserApi
}

public expect class SpotifyRemoteConnectionParams(clientId: String, redirectUri: String)