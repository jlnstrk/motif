package de.julianostarek.motif.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class BackendAuth(
    val accessToken: BackendAuthToken,
    val refreshToken: BackendAuthToken
)