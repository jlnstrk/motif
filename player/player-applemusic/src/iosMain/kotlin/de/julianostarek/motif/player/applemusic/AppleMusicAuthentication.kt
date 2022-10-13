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
    private val externalScope: CoroutineScope
) {
    private val _result: MutableStateFlow<AppleMusicAuthenticationResult> =
        MutableStateFlow(AppleMusicAuthenticationResult.NotDetermined)
    public actual val result: StateFlow<AppleMusicAuthenticationResult> get() = _result

    public suspend fun requestAuthorization() {
        var authorizationStatus = SKCloudServiceController.authorizationStatus()
        if (authorizationStatus == SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusNotDetermined) {
            authorizationStatus = suspendCoroutine { continuation ->
                SKCloudServiceController.requestAuthorization(continuation::resume)
            }
        }
        when (authorizationStatus) {
            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusAuthorized -> {
                val capabilities: ULong = suspendCoroutine {  continuation ->
                    SKCloudServiceController().requestCapabilitiesWithCompletionHandler { capabilities, nsError ->
                        if (nsError != null) {
                            continuation.resumeWithException(RuntimeException(nsError.localizedDescription))
                        }
                        continuation.resume(capabilities)
                    }
                }
                if (capabilities and SKCloudServiceCapabilityMusicCatalogPlayback == SKCloudServiceCapabilityMusicCatalogPlayback) {
                    _result.value = AppleMusicAuthenticationResult.Success(MusicPlayerController(externalScope))
                    return
                }
                if (capabilities and SKCloudServiceCapabilityMusicCatalogSubscriptionEligible == SKCloudServiceCapabilityMusicCatalogSubscriptionEligible) {
                    _result.value = AppleMusicAuthenticationResult.Error(AppleMusicAuthenticationError.NO_SUBSCRIPTION)
                    return
                }
                _result.value = AppleMusicAuthenticationResult.Error(AppleMusicAuthenticationError.UNKNOWN)
            }

            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusNotDetermined -> {
                _result.value = AppleMusicAuthenticationResult.NotDetermined
            }

            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusRestricted,
            SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusDenied -> {
                _result.value = AppleMusicAuthenticationResult.Error(AppleMusicAuthenticationError.USER_RESTRICTED)
            }

            else -> throw IllegalStateException()
        }
    }
}