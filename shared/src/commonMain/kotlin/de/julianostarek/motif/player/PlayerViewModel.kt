package de.julianostarek.motif.player

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.feed.domain.Motif
import de.julianostarek.motif.player.matching.MatchingCredentialsProvider
import de.julianostarek.motif.player.matching.playFromIsrc
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.*
import org.koin.core.scope.Scope

class PlayerViewModel : SharedViewModel(), KoinScopeComponent {
    override val scope: Scope by lazy {
        val scope = createScope(this)
        scope.declare(viewModelScope)
        scope
    }
    private val serviceAvailabilityInfo: PlayerServiceAvailabilityInfo by inject()
    private val credentialsProvider: MatchingCredentialsProvider by inject()

    val playerNegotiation: PlayerNegotiation by inject()
    private var negotiationState: PlayerNegotiation.State = PlayerNegotiation.State.NotConnected
    private val _frontendState: MutableStateFlow<FrontendState> = MutableStateFlow(FrontendState.Disconnected)

    val availableServices: List<PlayerServiceAvailabilityInfo.ServiceStatus> get() = serviceAvailabilityInfo.availableServices()
    val frontendState: StateFlow<FrontendState> get() = _frontendState

    init {
        viewModelScope.launch {
            playerNegotiation.state
                .flatMapLatest { state ->
                    this@PlayerViewModel.negotiationState = state
                    println(state)

                    flow {
                        emit(state to null)
                        (state as? PlayerNegotiation.State.Connected)?.player?.playerState()?.let { playerStateFlow ->
                            emitAll(playerStateFlow.map { state to it })
                        }
                    }
                }
                .collectLatest { (state, playerState) -> updateFrontendState(state, playerState) }
        }
    }

    fun isConnected(): Boolean = negotiationState is PlayerNegotiation.State.Connected

    fun playerOrNull(): Player? = (negotiationState as? PlayerNegotiation.State.Connected)?.player

    fun selectPlayer(service: PlayerService) {
        playerNegotiation.setService(service)
    }

    fun play(motif: Motif) = viewModelScope.launch {
        if (!isConnected()) return@launch
        playerNegotiation.playerOrNull()?.playFromIsrc(credentialsProvider, motif.isrc)
    }

    fun resume() = viewModelScope.launch {
        playerOrNull()?.resume()
    }

    fun pause() = viewModelScope.launch {
        playerOrNull()?.pause()
    }

    fun seekTo(position: Long) = viewModelScope.launch {
        playerOrNull()?.seekTo(position)
    }

    fun disconnect() {
        playerNegotiation.disconnect()
    }

    private fun updateFrontendState(negotiationState: PlayerNegotiation.State, playerStateOrNull: PlayerState?) {
        println("playerState: $playerStateOrNull")
        _frontendState.value = when (negotiationState) {
            is PlayerNegotiation.State.Connected -> {
                playerStateOrNull?.let { playerState ->
                    playerState.track?.let { track ->
                        FrontendState.Connected.Playback(
                            negotiationState.service,
                            track = track,
                            playerState.state != PlayerState.PlaybackState.PLAYING,
                            playerState.position.toInt(),
                            isMotif = true
                        )
                    }
                } ?: FrontendState.Connected.NoPlayback(
                    negotiationState.service
                )
            }

            is PlayerNegotiation.State.Connecting -> FrontendState.Connecting(negotiationState.service)
            is PlayerNegotiation.State.Error,
            PlayerNegotiation.State.NotConnected -> FrontendState.Disconnected
        }
    }

    override fun clear() {
        super.clear()
        playerNegotiation.disconnect()
        scope.close()
    }
}