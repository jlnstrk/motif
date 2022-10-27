package de.julianostarek.motif.player

public actual interface PlatformControls {
    public suspend fun trackImage(track: PlayerTrack, size: Int): AndroidTrackImage?
}