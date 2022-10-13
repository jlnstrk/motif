package de.julianostarek.motif.player

public interface PlayerTrack {
    public val title: String
    public val album: String
    public val artists: List<String>
    public val duration: Long
}