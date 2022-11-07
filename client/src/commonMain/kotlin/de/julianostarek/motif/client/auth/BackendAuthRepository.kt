/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.julianostarek.motif.client.auth

import co.touchlab.kermit.Logger
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
        var serviceToken = auth?.serviceTokens?.firstOrNull { it.service == service } ?: kotlin.run {
            Logger.i("Expected $service service auth but none is held")
            return null
        }
        if (!serviceToken.token.isValid()) {
            serviceToken = authClient.refreshServiceAuth(service) ?: kotlin.run {
                Logger.i("Failed to refresh $service service auth")
                return null
            }
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
            auth = authClient.refreshAppAuth(it.appToken)
            auth?.let { newAuth -> authStore.persistAuth(newAuth) }
        }
    }
}