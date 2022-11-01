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

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

public expect class AppleMusicAuthentication {
    public val result: StateFlow<AppleMusicAuthenticationResult>

    public fun invalidate()
}

@JvmInline
public value class AppleMusicDeveloperToken(
    public val token: String
)

@JvmInline
public value class AppleMusicUserToken(
    public val token: String
)

public enum class AppleMusicAuthenticationError {
    UNKNOWN,
    NO_SUBSCRIPTION,
    DEVELOPER_TOKEN,
    USER_CANCELLED,
    USER_RESTRICTED
}

public sealed class AppleMusicAuthenticationResult {
    public object NotDetermined : AppleMusicAuthenticationResult()
    public data class Success(public val controller: MusicPlayerController) : AppleMusicAuthenticationResult()
    public data class Error(public val error: AppleMusicAuthenticationError) : AppleMusicAuthenticationResult()
}