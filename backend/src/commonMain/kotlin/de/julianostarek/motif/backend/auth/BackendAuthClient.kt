package de.julianostarek.motif.backend.auth

import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpCall
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.post
import de.julianostarek.motif.backend.BackendConfig
import de.julianostarek.motif.backend.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.coroutines.CoroutineContext

@Single
class BackendAuthClient(
    private val config: BackendConfig
) {
    private val engine: HttpEngine = DefaultHttpEngine()

    suspend fun refreshAuth(refreshToken: BackendAuthToken): BackendAuth? {
        val request = BackendAuthTokenBody(refreshToken.value)
        return when (val result = call<_, BackendAuthResponse>(HttpMethod.Post, "/auth/token/refresh", request)) {
            is Response.Success -> result.data.auth
            is Response.Error -> {
                println("Failed to refresh token: Status ${result.statusCode}")
                null
            }
        }
    }

    suspend fun revokeAuth(accessToken: BackendAuthToken, refreshToken: BackendAuthToken): Boolean {
        val request = BackendAuthTokenBody(refreshToken.value)
        return when (val result = call<_, BackendRevokeResponse>(HttpMethod.Post, "/auth/token/refresh", request) {
            addHeader("Authorization", "Bearer ${accessToken.value}")
        }) {
            is Response.Success -> true
            is Response.Error -> {
                println("Failed to revoke refresh token: Status ${result.statusCode}")
                false
            }
        }
    }

    suspend fun spotifyCallback(callbackUrl: String): BackendAuth? {
        val query = callbackUrl.substringAfter('?')
        return when (val result = call<Unit, BackendAuthResponse>(HttpMethod.Get, "/auth/spotify/callback?$query")) {
            is Response.Success -> result.data.auth
            is Response.Error -> {
                println("Failed to redeem spotify callback: Status ${result.statusCode}")
                null
            }
        }
    }

    fun spotifyAuthUrl(): String {
        return config.restServerUrl + "/auth/spotify"
    }


    private suspend inline fun <reified Req, reified Res> call(
        method: HttpMethod,
        path: String,
        body: Req? = null,
        config: HttpCall.() -> Unit = {}
    ): Response<Res> {
        val response = HttpCall(engine, method, this@BackendAuthClient.config.restServerUrl + path)
            .apply {
                body?.let {
                    val serialized = Json.encodeToString(body)
                    body(serialized.toJsonBody())
                }
            }
            .apply(config)
            .let { call -> withContext(Dispatchers.Default) { call.execute() } }
        if (response.statusCode != 200) {
            return Response.Error(response.statusCode)
        }
        val deserialized = Json.decodeFromString<Res>(response.body!!.readUtf8())
        return Response.Success(deserialized)
    }

    sealed interface Response<D> {
        class Error<D>(val statusCode: Int) : Response<D>
        class Success<D>(val data: D) : Response<D>
    }

    private fun String.toJsonBody(): HttpBody = ByteStringHttpBody("application/json", this)
}