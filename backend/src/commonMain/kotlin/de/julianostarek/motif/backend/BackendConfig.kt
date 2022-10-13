package de.julianostarek.motif.backend

data class BackendConfig(
    val restServerUrl: String,
    val httpGraphQlServerUrl: String,
    val wsGraphQlServerUrl: String
)