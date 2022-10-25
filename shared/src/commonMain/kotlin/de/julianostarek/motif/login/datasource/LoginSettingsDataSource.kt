package de.julianostarek.motif.login.datasource

import de.julianostarek.motif.client.auth.BackendAuth
import kotlinx.coroutines.flow.Flow

interface LoginSettingsDataSource {
    val authChanged: Flow<BackendAuth?>
    suspend fun getAuth(): BackendAuth?
    suspend fun persistAuth(auth: BackendAuth?)
}