package de.julianostarek.motif.player

import de.julianostarek.motif.player.applemusic.AppleMusicAuthentication
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector

public interface PlayerConnector {
    public suspend fun connectAppleMusic(
        appleMusicAuthentication: AppleMusicAuthentication
    )
    public suspend fun connectSpotify(
        spotifyRemoteConnector: SpotifyRemoteConnector
    )
}