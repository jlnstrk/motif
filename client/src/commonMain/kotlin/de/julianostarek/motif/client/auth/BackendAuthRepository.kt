package de.julianostarek.motif.client.auth

import kotlinx.datetime.Clock
import org.koin.core.annotation.Single

@Single
class BackendAuthRepository(
    private val authStore: BackendAuthStore,
    private val authClient: BackendAuthClient
) {
    internal var auth: BackendAuth? = null
        private set

    fun getAppAuth(): AuthTokenWithRefresh? = auth?.appToken

    suspend fun getAppAuthOptionalRefresh(): AuthTokenWithRefresh {
        if (auth == null) {
            auth = authStore.getAuth()
            if (auth == null) {
                throw IllegalStateException("Authentication missing!")
            }
            if (auth?.appToken?.isValid() != true) {
                refreshAuth()
                if (auth?.appToken?.isValid() != true) {
                    authStore.invalidateAuth()
                    throw IllegalStateException("Refresh token expired!")
                }
            }
        }
        return auth!!.appToken
    }

    fun getServiceAuth(service: Service): AuthToken? = auth?.serviceTokens?.firstOrNull { it.service == service }?.token

    suspend fun getServiceAuthOptionalRefresh(service: Service): AuthToken? {
        // If needed, refresh app auth
        getAppAuthOptionalRefresh()

        // Try to get service tokens from auth
        var serviceToken = auth?.serviceTokens?.firstOrNull { it.service == service } ?: return null
        if (!serviceToken.token.isValid()) {
            serviceToken = authClient.refreshServiceAuth(service) ?: return null
            auth = auth?.copy(
                serviceTokens = auth?.serviceTokens
                    ?.filterNot { it.service == service }
                    .orEmpty() + serviceToken
            )
            auth?.let { authStore.persistAuth(it) }
        }
        return serviceToken.token
    }

    private fun AuthToken.isValid(): Boolean = accessTokenExpires?.let { Clock.System.now() < it } ?: true

    private fun AuthTokenWithRefresh.isValid(): Boolean = accessTokenExpires?.let { Clock.System.now() < it } ?: true

    suspend fun refreshAuth() {
        auth?.let {
            if (it.appToken.isValid()) {
                auth = authClient.refreshAppAuth(it.appToken)
                auth?.let { newAuth -> authStore.persistAuth(newAuth) }
            }
        }
    }
}