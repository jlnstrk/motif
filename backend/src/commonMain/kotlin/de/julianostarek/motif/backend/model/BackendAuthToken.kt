package de.julianostarek.motif.backend.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BackendAuthToken(
    val value: String,
    val expires: Instant
)