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