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

package de.julianostarek.motif.player.applemusic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.StoreKit.SKCloudServiceAuthorizationStatus
import platform.StoreKit.SKCloudServiceCapabilityMusicCatalogPlayback
import platform.StoreKit.SKCloudServiceCapabilityMusicCatalogSubscriptionEligible
import platform.StoreKit.SKCloudServiceController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

public actual class AppleMusicAuthentication(
    private val developerToken: AppleMusicDeveloperToken,
    private val externalScope: CoroutineScope
) {
    private val cloudServiceController: SKCloudServiceController = SKCloudServiceController()
    private val _result: MutableStateFlow<AppleMusicAuthenticationStatus> =
        MutableStateFlow(AppleMusicAuthenticationStatus.NotDetermined)
    public actual val status: StateFlow<AppleMusicAuthenticationStatus> get() = _result

    public suspend fun requestAuthorization() {
        var authorizationStatus = SKCloudServiceController.authorizationStatus()
        if (authorizationStatus == SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusNotDetermined) {
            authorizationStatus = suspendCoroutine { continuation ->
                SKCloudServiceController.requestAuthorization(continuation::resume)
            }
        }
        _result.value = when (authorizationStatus) {
            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusAuthorized -> {
                val capabilities: ULong = suspendCoroutine { continuation ->
                    cloudServiceController.requestCapabilitiesWithCompletionHandler { capabilities, nsError ->
                        if (nsError != null) {
                            continuation.resumeWithException(RuntimeException(nsError.localizedDescription))
                        } else {
                            continuation.resume(capabilities)
                        }
                    }
                }
                when {
                    capabilities and SKCloudServiceCapabilityMusicCatalogPlayback == SKCloudServiceCapabilityMusicCatalogPlayback -> {
                        val musicUserToken = requestMusicUserToken()
                        if (musicUserToken != null) {
                            AppleMusicAuthenticationStatus.Success(
                                controller = MusicPlayerController(externalScope),
                                musicUserToken = musicUserToken
                            )
                        } else {
                            AppleMusicAuthenticationStatus.Error(AppleMusicAuthenticationError.DEVELOPER_TOKEN)
                        }
                    }

                    capabilities and SKCloudServiceCapabilityMusicCatalogSubscriptionEligible == SKCloudServiceCapabilityMusicCatalogSubscriptionEligible -> AppleMusicAuthenticationStatus.Error(
                        AppleMusicAuthenticationError.NO_SUBSCRIPTION
                    )

                    else -> AppleMusicAuthenticationStatus.Error(AppleMusicAuthenticationError.UNKNOWN)
                }
            }

            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusNotDetermined -> AppleMusicAuthenticationStatus.NotDetermined
            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusRestricted,
            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusDenied -> AppleMusicAuthenticationStatus.Error(
                AppleMusicAuthenticationError.USER_RESTRICTED
            )

            else -> throw IllegalStateException()
        }
    }

    private suspend fun requestMusicUserToken(): AppleMusicUserToken? {
        try {
            val tokenString = suspendCoroutine { continuation ->
                cloudServiceController.requestUserTokenForDeveloperToken(
                    developerToken = developerToken.token
                ) { token, nsError ->
                    if (nsError != null) {
                        continuation.resumeWithException(RuntimeException(nsError.localizedDescription))
                    } else {
                        continuation.resume(token)
                    }
                }
            }
            return tokenString?.let(::AppleMusicUserToken)
        } catch (e: Exception) {
            return null
        }
    }

    public actual fun invalidate() {
        _result.value = AppleMusicAuthenticationStatus.NotDetermined
    }
}