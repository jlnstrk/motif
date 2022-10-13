package de.julianostarek.motif.player

import de.julianostarek.motif.player.spotify.Track

sealed class FrontendState {
    object Disconnected : FrontendState()
    sealed class Connected : FrontendState() {
        object NoPlayback : Connected()
        data class Playback(
            val track: Track,
            val isPaused: Boolean,
            val position: Int,
            val isMotif: Boolean
        ) : Connected()
    }
}