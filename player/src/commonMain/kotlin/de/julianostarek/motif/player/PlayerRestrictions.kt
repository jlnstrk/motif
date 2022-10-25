package de.julianostarek.motif.player

public sealed interface PlayerRestrictions {
    public val canSkipNext: Boolean
    public val canSkipPrev: Boolean
    public val canRepeatTrack: Boolean
    public val canRepeatContext: Boolean
    public val canToggleShuffle: Boolean
    public val canSeek: Boolean
}