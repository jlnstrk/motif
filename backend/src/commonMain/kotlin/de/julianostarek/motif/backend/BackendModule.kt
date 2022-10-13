package de.julianostarek.motif.backend

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketEngine
import de.julianostarek.motif.backend.adapter.TimestampAdapter
import de.julianostarek.motif.backend.auth.BackendAuthExpiredException
import de.julianostarek.motif.backend.auth.BackendAuthInterceptor
import de.julianostarek.motif.backend.auth.BackendAuthStore
import kotlinx.datetime.Instant
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan("de.julianostarek.motif.backend")
class BackendModule {
    @Single
    @Named("MotifClient")
    fun apolloClient(
        config: BackendConfig,
        authInterceptor: BackendAuthInterceptor
    ): ApolloClient {
        return ApolloClient.Builder()
            .httpServerUrl(config.httpGraphQlServerUrl)
            .webSocketServerUrl(config.wsGraphQlServerUrl)
            .wsProtocol(GraphQLWsProtocol.Factory(connectionPayload = {
                mapOf("token" to authInterceptor.getAuthOptRefresh().value)
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
            .addCustomScalarAdapter(CustomScalarType("Timestamp", Instant::class.qualifiedName!!), TimestampAdapter)
            .build()
    }
}