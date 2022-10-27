package de.julianostarek.motif.player

import de.julianostarek.motif.player.spotify.SpotifyRemote
import platform.UIKit.UIImage

public actual class SpotifyPlatformControls actual constructor(public val backing: SpotifyRemote) : PlatformControls {
    override suspend fun trackImage(track: PlayerTrack, size: Int): UIImage? {
        if (track !is SpotifyPlayerTrack) return null
        return backing.imagesApi.getImage(track.backing, size, size)
    }
}