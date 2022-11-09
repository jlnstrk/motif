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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import com.apple.android.music.playback.controller.MediaPlayerControllerFactory
import com.apple.android.sdk.authentication.AuthenticationFactory
import com.apple.android.sdk.authentication.AuthenticationManager
import com.apple.android.sdk.authentication.TokenError
import com.apple.android.sdk.authentication.TokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public fun isAppleMusicInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.apple.android.music", 0)
        true
    } catch (_: NameNotFoundException) {
        false
    }
}

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
                TokenError.UNKNOWN, null -> AppleMusicAuthenticationError.UNKNOWN
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

    public actual fun invalidate() {
        _result.value = AppleMusicAuthenticationResult.NotDetermined
    }

    private companion object {
        init {
            System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
            System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
            System.loadLibrary("c++_shared")
            System.loadLibrary("appleMusicSDK")
        }
    }
}