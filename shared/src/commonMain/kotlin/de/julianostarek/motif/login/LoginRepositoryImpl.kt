package de.julianostarek.motif.login

import de.julianostarek.motif.client.auth.*
import de.julianostarek.motif.login.datasource.LoginSettingsDataSource
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class LoginRepositoryImpl(
    private val backendAuthClient: BackendAuthClient,
    private val settings: LoginSettingsDataSource
) : LoginRepository, BackendAuthStore {
    override val auth: Flow<BackendAuth?>
        get() = settings.authChanged

    override fun loginUrl(service: Service): String = when (service) {
        Service.AppleMusic -> backendAuthClient.appleAuthUrl()
        Service.Spotify -> backendAuthClient.spotifyAuthUrl()
    }

    override suspend fun loginFromCallback(callbackUrl: String) {
        val auth = backendAuthClient.serviceCallback(callbackUrl)
        auth?.let { settings.persistAuth(it) }
    }

    override suspend fun getAuth(): BackendAuth? = settings.getAuth()

    override suspend fun persistAuth(auth: BackendAuth) = settings.persistAuth(auth)

    override suspend fun invalidateAuth() = settings.persistAuth(null)
}