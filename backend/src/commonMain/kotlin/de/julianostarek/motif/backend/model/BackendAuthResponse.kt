package de.julianostarek.motif.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class BackendAuthResponse(
    val auth: BackendAuth
)

