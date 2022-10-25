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