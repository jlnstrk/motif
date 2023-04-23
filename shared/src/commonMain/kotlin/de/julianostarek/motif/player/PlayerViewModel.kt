/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.julianostarek.motif.player

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.domain.Motif
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
    private val _remoteState: MutableStateFlow<RemoteState> = MutableStateFlow(RemoteState.Disconnected)

    val availableServices: List<PlayerServiceAvailabilityInfo.ServiceStatus> get() = serviceAvailabilityInfo.availableServices()
    @NativeCoroutinesState
    val remoteState: StateFlow<RemoteState> get() = _remoteState

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
        /*
        TODO: Hot swap
        val remoteState = remoteState.value
        if (remoteState is RemoteState.Connected.Playback) {
            viewModelScope.launch {
                withTimeoutOrNull(2500) {
                    playerNegotiation.state.filterIsInstance<PlayerNegotiation.State.Connected>()
                        .filter { it.service == service }
                        .first()
                }?.apply {
                    player.playFromIsrc(credentialsProvider, ) // ISRC?
                }
            }
        }*/
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
        println("remoteState: $playerStateOrNull")
        _remoteState.value = when (negotiationState) {
            is PlayerNegotiation.State.Connected -> {
                playerStateOrNull?.let { playerState ->
                    playerState.track?.let { track ->
                        RemoteState.Connected.Playback(
                            negotiationState.service,
                            track = track,
                            playerState.state != PlayerState.PlaybackState.PLAYING,
                            playerState.position.toInt(),
                            isMotif = true
                        )
                    }
                } ?: RemoteState.Connected.NoPlayback(
                    negotiationState.service
                )
            }

            is PlayerNegotiation.State.Connecting -> RemoteState.Connecting(negotiationState.service)
            is PlayerNegotiation.State.Error,
            PlayerNegotiation.State.NotConnected -> RemoteState.Disconnected
        }
    }

    override fun clear() {
        super.clear()
        playerNegotiation.disconnect()
        scope.close()
    }
}