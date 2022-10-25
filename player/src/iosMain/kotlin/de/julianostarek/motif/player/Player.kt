package de.julianostarek.motif.player

import platform.UIKit.UIImage

public suspend fun Player.trackImage(track: PlayerTrack, size: Int): UIImage? {
    return when (this) {
        is AppleMusicPlayer -> {
            if (track !is AppleMusicPlayerTrack) return null
            track.backing.artwork(size, size)
        }
        is SpotifyPlayer -> {
            if (track !is SpotifyPlayerTrack) return null
            backing.imagesApi.getImage(track.backing, size, size)
        }
    }
}