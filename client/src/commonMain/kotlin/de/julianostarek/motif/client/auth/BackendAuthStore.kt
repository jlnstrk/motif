package de.julianostarek.motif.client.auth

interface BackendAuthStore {
    suspend fun persistAuth(auth: BackendAuth)
    suspend fun getAuth(): BackendAuth?
    suspend fun invalidateAuth()
}