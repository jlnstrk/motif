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

package de.julianostarek.motif.login

import de.julianostarek.motif.client.auth.*
import de.julianostarek.motif.datasource.LoginLocalDataSource
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class LoginRepositoryImpl(
    private val backendAuthClient: BackendAuthClient,
    private val settings: LoginLocalDataSource
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