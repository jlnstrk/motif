package de.julianostarek.motif.player

sealed class FrontendState {
    object Disconnected : FrontendState()
    sealed class Connected : FrontendState() {
        abstract val service: PlayerService

        data class NoPlayback(
            override val service: PlayerService
        ) : Connected()

        data class Playback(
            override val service: PlayerService,
            val track: PlayerTrack,
            val isPaused: Boolean,
            val position: Int,
            val isMotif: Boolean
        ) : Connected()
    }
}