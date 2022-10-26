package de.julianostarek.motif.login

import de.julianostarek.motif.client.auth.BackendAuth
import de.julianostarek.motif.client.auth.Service
import kotlinx.coroutines.flow.Flow

interface LoginRepository {
    val auth: Flow<BackendAuth?>
    fun loginUrl(service: Service): String
    suspend fun loginFromCallback(callbackUrl: String)
}