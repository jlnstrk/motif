package de.julianostarek.motif.client.auth

import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpCall
import com.apollographql.apollo3.network.http.HttpEngine
import de.julianostarek.motif.client.BackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class BackendAuthClient(
    private val config: BackendConfig
) {
    private val engine: HttpEngine = DefaultHttpEngine()

    suspend fun refreshAppAuth(appToken: AuthTokenWithRefresh): BackendAuth? {
        val request = BackendTokenPayload(appToken.refreshToken)
        return when (val result = call<_, BackendAuth>(HttpMethod.Post, "/auth/token/refresh", request)) {
            is Response.Success -> result.data
            is Response.Error -> {
                println("Failed to refresh token: Status ${result.statusCode}")
                null
            }
        }
    }

    suspend fun revokeAppAuth(appToken: AuthTokenWithRefresh): Boolean {
        val request = BackendTokenPayload(appToken.refreshToken)
        return when (val result = call<_, Unit>(HttpMethod.Post, "/auth/token/refresh", request) {
            addHeader("Authorization", "Bearer ${appToken.accessToken}")
        }) {
            is Response.Success -> true
            is Response.Error -> {
                println("Failed to revoke refresh token: Status ${result.statusCode}")
                false
            }
        }
    }

    suspend fun refreshServiceAuth(service: Service): ServiceToken? {
        val servicePathSeg = when (service) {
            Service.AppleMusic -> "apple"
            Service.Spotify -> "spotify"
        }
        return when (val result = call<Unit, ServiceTokenResponse>(HttpMethod.Post, "/auth/$servicePathSeg/refresh")) {
            is Response.Success -> result.data.serviceToken
            is Response.Error -> {
                println("Failed to refresh $servicePathSeg auth: Status ${result.statusCode}")
                null
            }
        }
    }

    suspend fun serviceCallback(callbackUrl: String): BackendAuth? {
        val path = when (callbackUrl.substringAfterLast('/').substringBefore('?')) {
            "apple" -> "/auth/apple/callback/mobile"
            "spotify" -> "/auth/spotify/callback"
            else -> throw IllegalArgumentException()
        }
        val query = callbackUrl.substringAfter('?')
        return when (val result = call<Unit, BackendAuth>(HttpMethod.Get, "$path?$query")) {
            is Response.Success -> result.data
            is Response.Error -> {
                println("Failed to redeem service callback: Status ${result.statusCode}")
                null
            }
        }
    }

    fun appleAuthUrl(): String {
        return config.restServerUrl + "/auth/apple?callbackMode=mobile"
    }

    fun spotifyAuthUrl(): String {
        return config.restServerUrl + "/auth/spotify?callbackMode=mobile"
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