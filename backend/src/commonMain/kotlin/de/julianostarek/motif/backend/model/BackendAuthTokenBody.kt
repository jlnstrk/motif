package de.julianostarek.motif.backend.model

import kotlinx.serialization.Serializable

@Serializable
class BackendAuthTokenBody(
    val token: String
)