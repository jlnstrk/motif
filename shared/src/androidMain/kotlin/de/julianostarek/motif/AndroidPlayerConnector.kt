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

package de.julianostarek.motif

import android.content.Context
import android.content.Intent
import de.julianostarek.motif.player.PlayerConnector
import de.julianostarek.motif.player.PlayerViewModel
import de.julianostarek.motif.player.applemusic.AppleMusicAuthentication
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scope(PlayerViewModel::class)
@Scoped
class AndroidPlayerConnector(
    private val context: Context
) : PlayerConnector {
    private val _playerConnectIntent: MutableSharedFlow<Intent> = MutableSharedFlow()
    val playerConnectIntent: SharedFlow<Intent> get() = _playerConnectIntent

    override suspend fun connectAppleMusic(appleMusicAuthentication: AppleMusicAuthentication) {
        val intent = appleMusicAuthentication.createIntent()
        _playerConnectIntent.emit(intent)
    }

    override suspend fun connectSpotify(spotifyRemoteConnector: SpotifyRemoteConnector) {
        spotifyRemoteConnector.connect(context)
    }
}