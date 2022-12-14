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

sealed class RemoteState {
    object Disconnected : RemoteState()
    sealed class ConnectingOrConnected : RemoteState() {
        abstract val service: PlayerService
    }
    data class Connecting(override val service: PlayerService) : ConnectingOrConnected()
    sealed class Connected : ConnectingOrConnected() {
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