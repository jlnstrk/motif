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

import de.julianostarek.motif.client.auth.BackendAuthRepository
import de.julianostarek.motif.client.auth.Service
import de.julianostarek.motif.player.matching.MatchingCredentials
import de.julianostarek.motif.player.matching.MatchingCredentialsProvider
import de.julianostarek.motif.shared.BuildKonfig
import org.koin.core.annotation.Single

@Single
class BackendMatchingCredentialsProvider(
    private val authRepository: BackendAuthRepository
) : MatchingCredentialsProvider {
    override suspend fun appleCredentials(): MatchingCredentials.AppleMusicCredentials? {
        return authRepository.getServiceAuthOptionalRefresh(Service.AppleMusic)?.let { token ->
            MatchingCredentials.AppleMusicCredentials(
                developerToken = BuildKonfig.APPLE_DEVELOPER_TOKEN,
                musicUserToken = token.accessToken
            )
        }
    }

    override suspend fun spotifyCredentials(): MatchingCredentials.SpotifyCredentials? {
        return authRepository.getServiceAuthOptionalRefresh(Service.Spotify)?.let { token ->
            MatchingCredentials.SpotifyCredentials(
                clientId = BuildKonfig.SPOTIFY_CLIENT_ID,
                // clientSecret = BuildKonfig.SPOTIFY_CLIENT_SECRET,
                accessToken = token.accessToken,
                expires = token.accessTokenExpires!!
            )
        }
    }
}