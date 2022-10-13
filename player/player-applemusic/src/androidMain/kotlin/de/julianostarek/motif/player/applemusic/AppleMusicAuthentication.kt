package de.julianostarek.motif.player.applemusic

import android.content.Context
import android.content.Intent
import com.apple.android.music.playback.controller.MediaPlayerController
import com.apple.android.music.playback.controller.MediaPlayerControllerFactory
import com.apple.android.sdk.authentication.AuthenticationFactory
import com.apple.android.sdk.authentication.AuthenticationManager
import com.apple.android.sdk.authentication.TokenError
import com.apple.android.sdk.authentication.TokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public actual class AppleMusicAuthentication(
    private val context: Context,
    private val developerToken: AppleMusicDeveloperToken,
    private val externalScope: CoroutineScope
) {
    private val manager: AuthenticationManager = AuthenticationFactory.createAuthenticationManager(context)

    public fun createIntent(): Intent = manager.createIntentBuilder(developerToken.token)
        .build()

    public fun handleIntent(intent: Intent) {
        val tokenResult = manager.handleTokenResult(intent)
        if (tokenResult.isError) {
            val error: AppleMusicAuthenticationError = when (tokenResult.error) {
                TokenError.USER_CANCELLED -> AppleMusicAuthenticationError.USER_CANCELLED
                TokenError.NO_SUBSCRIPTION,
                TokenError.SUBSCRIPTION_EXPIRED -> AppleMusicAuthenticationError.NO_SUBSCRIPTION

                TokenError.TOKEN_FETCH_ERROR -> AppleMusicAuthenticationError.DEVELOPER_TOKEN
                TokenError.UNKNOWN -> AppleMusicAuthenticationError.UNKNOWN
            }
            _result.value = AppleMusicAuthenticationResult.Error(error)
        } else {
            val tokenProvider = object : TokenProvider {
                override fun getDeveloperToken(): String = this@AppleMusicAuthentication.developerToken.token
                override fun getUserToken(): String = tokenResult.musicUserToken
            }
            val controller = MediaPlayerControllerFactory.createLocalController(context, tokenProvider)
            val musicPlayer = MusicPlayerController(controller, externalScope)
            _result.value = AppleMusicAuthenticationResult.Success(musicPlayer)
        }
    }

    private val _result: MutableStateFlow<AppleMusicAuthenticationResult> =
        MutableStateFlow(AppleMusicAuthenticationResult.NotDetermined)
    public actual val result: StateFlow<AppleMusicAuthenticationResult> get() = _result
}