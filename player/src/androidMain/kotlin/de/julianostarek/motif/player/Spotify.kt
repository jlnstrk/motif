package de.julianostarek.motif.player

import de.julianostarek.motif.player.spotify.ImageDimension
import de.julianostarek.motif.player.spotify.SpotifyRemote

public actual class SpotifyPlatformControls actual constructor(public val backing: SpotifyRemote) : PlatformControls {
    override suspend fun trackImage(track: PlayerTrack, size: Int): AndroidTrackImage? {
        if (track !is SpotifyPlayerTrack) return null
        // TODO: Use upper bound < size
        return backing.imagesApi.getImage(track.backing, ImageDimension.LARGE)
            .let(AndroidTrackImage::Bitmap)
    }
}