package de.julianostarek.motif

import com.russhwolf.settings.ObservableSettings
import de.julianostarek.motif.backend.BackendConfig
import de.julianostarek.motif.backend.BackendModule
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnectionParams
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector
import de.julianostarek.motif.persist.DriverFactory
import de.julianostarek.motif.persist.createDatabase
import de.julianostarek.motif.shared.BuildKonfig
import kotlinx.coroutines.GlobalScope
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ksp.generated.module

@Module
@ComponentScan("de.julianostarek.motif")
class SharedModule {
    @Single
    fun backendConfig(): BackendConfig {
        val httpScheme = "https" // "http"
        val wsScheme = "wss" // "ws
        val host = "motif.fly.dev" // "192.168.188.22:8080"
        return BackendConfig(
            restServerUrl = "$httpScheme://$host",
            httpGraphQlServerUrl = "$httpScheme://$host/graphql",
            wsGraphQlServerUrl = "$wsScheme://$host/graphql"
        )
    }

    @Single
    fun spotifyParams(): SpotifyRemoteConnectionParams {
        return SpotifyRemoteConnectionParams(
            clientId = BuildKonfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildKonfig.SPOTIFY_CALLBACK_URI
        )
    }
}

internal fun sharedStartKoin(driverFactory: DriverFactory, loginSettings: ObservableSettings) {
    startKoin {
        modules(SharedModule().module, BackendModule().module, module {
            single { createDatabase(driverFactory) }
            single(qualifier = named("LoginSettings")) { loginSettings }
            single { SpotifyRemoteConnector(get(), GlobalScope) }
        })
    }
}