package de.julianostarek.motif.player

public actual class AppleMusicPlatformControls : PlatformControls {
    override suspend fun trackImage(track: PlayerTrack, size: Int): AndroidTrackImage? {
        if (track !is AppleMusicPlayerTrack) return null
        return track.backing.artworkUrl(size, size)
            ?.let(AndroidTrackImage::Url)
    }
}