package de.julianostarek.motif.client.auth

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BackendAuth(
    val appToken: AuthTokenWithRefresh,
    val serviceTokens: List<ServiceToken>
)

@Serializable
data class AuthToken(
    val accessToken: String,
    val accessTokenExpires: Instant?
)

@Serializable
data class AuthTokenWithRefresh(
    val accessToken: String,
    val accessTokenExpires: Instant?,
    val refreshToken: String
)

@Serializable
enum class Service {
    AppleMusic,
    Spotify
}

@Serializable
data class ServiceTokenResponse(
    val serviceToken: ServiceToken
)

@Serializable
data class ServiceToken(
    val service: Service,
    val serviceId: String,
    val token: AuthToken
)

@Serializable
data class BackendTokenPayload(
    val token: String
)

