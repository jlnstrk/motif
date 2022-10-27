package de.julianostarek.motif.player

import platform.UIKit.UIImage

public actual interface PlatformControls {
    public suspend fun trackImage(track: PlayerTrack, size: Int): UIImage?
}