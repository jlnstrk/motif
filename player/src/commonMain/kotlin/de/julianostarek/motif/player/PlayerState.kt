package de.julianostarek.motif.player

public sealed interface PlayerState {
    public val track: PlayerTrack?
    public val state: PlaybackState
    public val position: Long
    public val repeatMode: RepeatMode
    public val shuffleMode: ShuffleMode

    public enum class PlaybackState {
        STOPPED, PAUSED, PLAYING
    }

    public enum class RepeatMode {
        OFF, ONE, ALL
    }

    public enum class ShuffleMode {
        OFF, ON
    }
}