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

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single
class BackendAuthInterceptor(
    private val repository: BackendAuthRepository
) : HttpInterceptor {
    private val mutex: Mutex = Mutex()

    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
        val response = chain.proceed(request.withAuth())
        if (response.statusCode == 401) {
            mutex.withLock { repository.refreshAuth() }
            return chain.proceed(request.withAuth())
        }
        return response
    }

    private suspend fun HttpRequest.withAuth(): HttpRequest {
        val token = mutex.withLock { repository.getAppAuthOptionalRefresh() }
        return newBuilder()
            .addHeader("Authorization", "Bearer ${token.accessToken}")
            .build()
    }
}