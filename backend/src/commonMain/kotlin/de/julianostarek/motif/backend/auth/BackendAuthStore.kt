package de.julianostarek.motif.backend.auth

import de.julianostarek.motif.backend.model.BackendAuth

interface BackendAuthStore {
    suspend fun persistAuth(auth: BackendAuth)
    suspend fun getAuth(): BackendAuth?
    suspend fun invalidateAuth()
}