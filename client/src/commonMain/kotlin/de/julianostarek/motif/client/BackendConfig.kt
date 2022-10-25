package de.julianostarek.motif.client

data class BackendConfig(
    val restServerUrl: String,
    val httpGraphQlServerUrl: String,
    val wsGraphQlServerUrl: String
)