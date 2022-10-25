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