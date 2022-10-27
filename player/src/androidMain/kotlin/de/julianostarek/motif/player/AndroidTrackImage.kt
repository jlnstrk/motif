package de.julianostarek.motif.player

public sealed interface AndroidTrackImage {
    @JvmInline
    public value class Bitmap(public val bitmap: android.graphics.Bitmap) : AndroidTrackImage
    @JvmInline
    public value class Url(public val url: String) : AndroidTrackImage
}