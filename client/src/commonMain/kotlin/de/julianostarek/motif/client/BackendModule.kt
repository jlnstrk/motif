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

package de.julianostarek.motif.client

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import de.julianostarek.motif.client.adapter.DateTimeAdapter
import de.julianostarek.motif.client.auth.BackendAuthExpiredException
import de.julianostarek.motif.client.auth.BackendAuthInterceptor
import de.julianostarek.motif.client.auth.BackendAuthRepository
import kotlinx.datetime.Instant
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan("de.julianostarek.motif.client")
class BackendModule {
    @Single
    @Named("MotifClient")
    fun apolloClient(
        config: BackendConfig,
        authRepository: BackendAuthRepository,
        authInterceptor: BackendAuthInterceptor
    ): ApolloClient {
        return ApolloClient.Builder()
            .httpServerUrl(config.httpGraphQlServerUrl)
            .webSocketServerUrl(config.wsGraphQlServerUrl)
            .wsProtocol(GraphQLWsProtocol.Factory(connectionPayload = {
                mapOf("token" to authRepository.getAppAuthOptionalRefresh().accessToken)
            }))
            .webSocketReopenWhen { throwable, attempt ->
                if (throwable is BackendAuthExpiredException) {
                    true
                } else {
                    // Another WebSocket error happened, decide what to do with it
                    // Here we're trying to reconnect at most 3 times
                    attempt < 3
                }
            }
            .addHttpInterceptor(authInterceptor)
            .webSocketIdleTimeoutMillis(Long.MAX_VALUE)
            .addCustomScalarAdapter(CustomScalarType("DateTime", Instant::class.qualifiedName!!), DateTimeAdapter)
            .build()
    }
}