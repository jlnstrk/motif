package de.julianostarek.motif

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import de.julianostarek.motif.client.BackendConfig
import de.julianostarek.motif.client.BackendModule
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnectionParams
import de.julianostarek.motif.player.spotify.SpotifyRemoteConnector
import de.julianostarek.motif.persist.DriverFactory
import de.julianostarek.motif.persist.MotifDatabase
import de.julianostarek.motif.persist.createDatabase
import de.julianostarek.motif.player.PlayerConnector
import de.julianostarek.motif.player.PlayerNegotiation
import de.julianostarek.motif.player.PlayerViewModel
import de.julianostarek.motif.player.applemusic.AppleMusicAuthentication
import de.julianostarek.motif.shared.BuildKonfig
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.*
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
        val host = "motif.julianostarek.de" // "192.168.188.22:8080"
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

    @Scope(PlayerViewModel::class)
    @Scoped
    fun spotifyConnector(
        params: SpotifyRemoteConnectionParams,
        externalScope: CoroutineScope
    ): SpotifyRemoteConnector {
        return SpotifyRemoteConnector(
            params,
            externalScope
        )
    }

    @Scope(PlayerViewModel::class)
    @Scoped
    fun playerNegotiation(
        appleMusicAuthentication: AppleMusicAuthentication,
        spotifyConnector: SpotifyRemoteConnector,
        connector: PlayerConnector,
        externalScope: CoroutineScope
    ): PlayerNegotiation {
        return PlayerNegotiation(
            appleMusicAuthentication,
            spotifyConnector,
            connector,
            externalScope
        )
    }

    @Single
    fun motifDatabase(
        driverFactory: DriverFactory
    ): MotifDatabase {
        return createDatabase(driverFactory)
    }
}

internal fun sharedStartKoin(
    platformModule: org.koin.core.module.Module
) {
    startKoin {
        modules(
            platformModule,
            SharedModule().module,
            BackendModule().module
        )
    }
}