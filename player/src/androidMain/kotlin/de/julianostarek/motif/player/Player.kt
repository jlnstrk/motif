package de.julianostarek.motif.player

import de.julianostarek.motif.player.spotify.ImageDimension

public suspend fun Player.trackImage(track: PlayerTrack, size: Int): AndroidTrackImage? {
    return when (this) {
        is AppleMusicPlayer -> {
            if (track !is AppleMusicPlayerTrack) return null
            track.backing.artworkUrl(size, size)
                ?.let(AndroidTrackImage::Url)
        }

        is SpotifyPlayer -> {
            if (track !is SpotifyPlayerTrack) return null
            // TODO: Use upper bound < size
            backing.imagesApi.getImage(track.backing, ImageDimension.LARGE)
                .let(AndroidTrackImage::Bitmap)
        }
    }
}

public sealed class AndroidTrackImage {
    public data class Bitmap(public val bitmap: android.graphics.Bitmap) : AndroidTrackImage()
    public data class Url(public val url: String) : AndroidTrackImage()
}