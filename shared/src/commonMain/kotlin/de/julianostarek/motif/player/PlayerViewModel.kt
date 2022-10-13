package de.julianostarek.motif.player

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.player.spotify.PlayerState
import de.julianostarek.motif.player.spotify.SpotifyRemote
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class PlayerViewModel : SharedViewModel(), KoinComponent {
    val spotifyConnector: SpotifyRemoteConnector by inject()
    val spotifyRemote: SpotifyRemote? get() = spotifyConnector.remote.value

    private val _frontendState: MutableStateFlow<FrontendState> = MutableStateFlow(FrontendState.Disconnected)
    val frontendState: StateFlow<FrontendState> get() = _frontendState

    init {
        viewModelScope.launch {
            spotifyConnector.remote.collectLatest { remote ->
                if (remote == null || !remote.isConnected) {
                    _frontendState.value = FrontendState.Disconnected
                } else {
                    remote.playerApi.playerState()
                        .onStart { updateFrontendState(remote.playerApi.getPlayerState()) }
                        .collectLatest(::updateFrontendState)
                }
            }
        }
    }

    private fun updateFrontendState(playerState: PlayerState?) {
        println("playerState: $playerState")
        val track = playerState?.track
        if (playerState == null || track == null) {
            _frontendState.value = FrontendState.Connected.NoPlayback
        } else {
            _frontendState.value = FrontendState.Connected.Playback(
                track = track,
                isPaused = playerState.isPaused,
                position = playerState.playbackPosition.toInt(),
                isMotif = false
            )
        }
    }
}