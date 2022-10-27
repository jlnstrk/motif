package de.julianostarek.motif.player

import platform.UIKit.UIImage

public actual class AppleMusicPlatformControls : PlatformControls {
    override suspend fun trackImage(track: PlayerTrack, size: Int): UIImage? {
        if (track !is AppleMusicPlayerTrack) return null
        return track.backing.artwork(size, size)
    }
}