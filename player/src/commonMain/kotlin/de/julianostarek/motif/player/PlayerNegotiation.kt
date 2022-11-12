package de.julianostarek.motif.player

import de.julianostarek.motif.player.applemusic.AppleMusicAuthentication
import de.julianostarek.motif.player.applemusic.AppleMusicAuthenticationStatus
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnectionState
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

public class PlayerNegotiation(
    public val appleMusicAuthentication: AppleMusicAuthentication,
    public val spotifyConnector: SpotifyRemoteConnector,
    private val connector: PlayerConnector,
    externalScope: CoroutineScope
) {
    private val _state: MutableStateFlow<State> = MutableStateFlow(State.NotConnected)
    public val state: StateFlow<State> get() = _state

    private val service: MutableStateFlow<PlayerService?> = MutableStateFlow(null)

    public fun playerOrNull(): Player? = (state.value as? State.Connected)?.player

    public fun setService(service: PlayerService) {
        this.service.value = service
    }

    public fun disconnect() {
        this.service.value = null
    }

    private suspend fun disconnectAppleMusic() {
        val appleResult = appleMusicAuthentication.status.value
        if (appleResult is AppleMusicAuthenticationStatus.Success) {
            appleResult.controller.stop()
            appleResult.controller.release()
        }
    }

    private suspend fun disconnectSpotify() {
        val spotifyState = spotifyConnector.state.value
        if (spotifyState is SpotifyRemoteConnectionState.Connected) {
            spotifyState.remote.playerApi.pause()
            spotifyConnector.disconnect()
        }
    }

    init {
        externalScope.launch {
            service.distinctUntilChanged { old, new -> old == new }
                .collectLatest { targetService ->
                    if (targetService != PlayerService.APPLE_MUSIC
                        && appleMusicAuthentication.status.value is AppleMusicAuthenticationStatus.Success
                    ) {
                        disconnectAppleMusic()
                    }
                    if (targetService != PlayerService.SPOTIFY
                        && spotifyConnector.state.value is SpotifyRemoteConnectionState.Connected
                    ) {
                        disconnectSpotify()
                    }
                    when (targetService) {
                        PlayerService.APPLE_MUSIC -> connector.connectAppleMusic(appleMusicAuthentication)
                        PlayerService.SPOTIFY -> connector.connectSpotify(spotifyConnector)
                        else -> {}
                    }
                }
        }
        externalScope.launch {
            combine(
                service,
                spotifyConnector.state,
                appleMusicAuthentication.status
            ) { targetService, spotifyState, appleResult ->
                when (targetService) {
                    PlayerService.APPLE_MUSIC -> when (appleResult) {
                        is AppleMusicAuthenticationStatus.Error -> State.Error(targetService, appleResult.error)
                        AppleMusicAuthenticationStatus.NotDetermined -> State.Connecting(targetService)
                        is AppleMusicAuthenticationStatus.Success -> {
                            val player = appleResult.controller.let(::AppleMusicPlayer)
                            State.Connected(targetService, player)
                        }
                    }

                    PlayerService.SPOTIFY -> when (spotifyState) {
                        is SpotifyRemoteConnectionState.Connected -> {
                            val player = spotifyState.remote.let(::SpotifyPlayer)
                            State.Connected(targetService, player)
                        }

                        is SpotifyRemoteConnectionState.Disconnected -> {
                            if (spotifyState.error != null) {
                                State.Error(targetService, spotifyState.error)
                            } else {
                                State.Connecting(targetService)
                            }
                        }

                        is SpotifyRemoteConnectionState.FailedToConnect -> State.Error(
                            targetService,
                            spotifyState.error
                        )
                    }

                    null -> State.NotConnected
                }
            }.collectLatest { state ->
                this@PlayerNegotiation._state.value = state
            }
        }
    }

    public sealed interface State {
        public object NotConnected : State

        public data class Connecting(
            public val service: PlayerService
        ) : State

        public data class Connected(
            public val service: PlayerService,
            public val player: Player
        ) : State

        public data class Error(
            public val service: PlayerService,
            public val error: Any? = null
        ) : State
    }
}