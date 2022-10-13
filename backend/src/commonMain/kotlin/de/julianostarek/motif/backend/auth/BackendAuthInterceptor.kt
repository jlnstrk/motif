package de.julianostarek.motif.backend.auth

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.*
import de.julianostarek.motif.backend.model.BackendAuth
import de.julianostarek.motif.backend.model.BackendAuthToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.koin.core.annotation.Single

@Single
class BackendAuthInterceptor(
    private val authStore: BackendAuthStore,
    private val authClient: BackendAuthClient
) : HttpInterceptor {
    private val mutex: Mutex = Mutex()
    internal var auth: BackendAuth? = null
        private set

    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
        val response = chain.proceed(request.withAuth())
        if (response.statusCode == 401) {
            mutex.withLock { refreshAuth() }
            return chain.proceed(request.withAuth())
        }

        return response
    }

    suspend fun getAuthOptRefresh(): BackendAuthToken = mutex.withLock {
        if (auth == null) {
            auth = authStore.getAuth()
            if (auth == null) {
                throw IllegalStateException("Authentication missing!")
            }
            if (auth?.accessToken?.isValid() != true) {
                refreshAuth()
                if (auth?.accessToken?.isValid() != true) {
                    authStore.invalidateAuth()
                    throw IllegalStateException("Refresh token expired!")
                }
            }
        }
        return auth!!.accessToken
    }

    private suspend fun HttpRequest.withAuth(): HttpRequest {
        val accessToken = getAuthOptRefresh()
        return newBuilder()
            .addHeader("Authorization", "Bearer ${accessToken.value}")
            .build()
    }

    private fun BackendAuthToken.isValid(): Boolean = Clock.System.now() < expires

    private suspend fun refreshAuth() {
        auth?.refreshToken?.let {
            if (it.isValid()) {
                auth = authClient.refreshAuth(it)
                auth?.let { newAuth -> authStore.persistAuth(newAuth) }
            }
        }
    }
}