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

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BackendAuth(
    val appToken: AuthTokenWithRefresh,
    val serviceTokens: List<ServiceToken>
)

@Serializable
data class AuthToken(
    val accessToken: String,
    val accessTokenExpires: Instant?
)

@Serializable
data class AuthTokenWithRefresh(
    val accessToken: String,
    val accessTokenExpires: Instant?,
    val refreshToken: String
)

@Serializable
enum class Service {
    AppleMusic,
    Spotify
}

@Serializable
data class ServiceTokenResponse(
    val serviceToken: ServiceToken
)

@Serializable
data class ServiceToken(
    val service: Service,
    val serviceId: String,
    val token: AuthToken
)

@Serializable
data class BackendTokenPayload(
    val token: String
)

