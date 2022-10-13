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
    public val remote: StateFlow<SpotifyRemote?>

    public fun disconnect()
}

public expect class SpotifyRemote {
    public val isConnected: Boolean
    public val playerApi: PlayerApi
    public val imagesApi: ImagesApi
    public val userApi: UserApi
}

public expect class SpotifyRemoteConnectionParams(clientId: String, redirectUri: String)