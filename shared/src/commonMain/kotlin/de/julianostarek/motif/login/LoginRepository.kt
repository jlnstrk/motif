package de.julianostarek.motif.login

import de.julianostarek.motif.backend.model.BackendAuth
import kotlinx.coroutines.flow.Flow

interface LoginRepository {
    val auth: Flow<BackendAuth?>
    fun loginUrl(): String
    suspend fun loginFromCallback(callbackUrl: String)
}