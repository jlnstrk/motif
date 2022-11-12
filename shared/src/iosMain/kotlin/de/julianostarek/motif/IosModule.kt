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

import com.russhwolf.settings.AppleSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import de.julianostarek.motif.client.auth.BackendAuthRepository
import de.julianostarek.motif.client.auth.Service
import de.julianostarek.motif.persist.DriverFactory
import de.julianostarek.motif.player.PlayerConnector
import de.julianostarek.motif.player.PlayerService
import de.julianostarek.motif.player.PlayerServiceAvailabilityInfo
import de.julianostarek.motif.player.PlayerViewModel
import de.julianostarek.motif.player.applemusic.AppleMusicAuthentication
import de.julianostarek.motif.player.applemusic.AppleMusicDeveloperToken
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector
import de.julianostarek.motif.player.spotify.isSpotifyInstalled
import de.julianostarek.motif.shared.BuildKonfig
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.*
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import org.koin.ksp.generated.module

@Module
class IosModule {

    @Scope(PlayerViewModel::class)
    @Scoped
    fun appleMusicAuthentication(
        externalScope: CoroutineScope
    ): AppleMusicAuthentication {
        return AppleMusicAuthentication(
            developerToken = AppleMusicDeveloperToken(BuildKonfig.APPLE_DEVELOPER_TOKEN),
            externalScope = externalScope,
        )
    }

    @Single
    fun playerServiceAvailabilityInfo(
        backendAuthRepository: BackendAuthRepository
    ): PlayerServiceAvailabilityInfo {
        return PlayerServiceAvailabilityInfo {
            listOf(
                PlayerServiceAvailabilityInfo.ServiceStatus(
                    service = PlayerService.APPLE_MUSIC,
                    isInstalled = true,
                    isLinked = backendAuthRepository.getServiceAuth(Service.AppleMusic) != null
                ),
                PlayerServiceAvailabilityInfo.ServiceStatus(
                    service = PlayerService.SPOTIFY,
                    isInstalled = isSpotifyInstalled(),
                    isLinked = backendAuthRepository.getServiceAuth(Service.Spotify) != null
                )
            )
        }
    }

    @Single
    fun playerConnector(): PlayerConnector {
        return object : PlayerConnector {
            override suspend fun connectAppleMusic(appleMusicAuthentication: AppleMusicAuthentication) {
                appleMusicAuthentication.requestAuthorization()
            }

            override suspend fun connectSpotify(spotifyRemoteConnector: SpotifyRemoteConnector) {
                spotifyRemoteConnector.connect()
            }
        }
    }

    @Single
    fun driverFactory(): DriverFactory = DriverFactory()

    @OptIn(ExperimentalSettingsApi::class)
    @Named("LoginSettings")
    @Single
    fun loginSettings(): ObservableSettings {
        val defaults = NSUserDefaults("login")
        return AppleSettings(defaults)
    }
}

fun iosStartKoin() {
    val iosModule = module {
        includes(IosModule().module)
    }

    sharedStartKoin(platformModule = iosModule)
}