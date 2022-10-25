package de.julianostarek.motif

import android.content.Context
import com.russhwolf.settings.AndroidSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import de.julianostarek.motif.client.auth.BackendAuthRepository
import de.julianostarek.motif.client.auth.Service
import de.julianostarek.motif.persist.DriverFactory
import de.julianostarek.motif.player.PlayerService
import de.julianostarek.motif.player.PlayerServiceAvailabilityInfo
import de.julianostarek.motif.player.PlayerViewModel
import de.julianostarek.motif.player.applemusic.AppleMusicAuthentication
import de.julianostarek.motif.player.applemusic.AppleMusicDeveloperToken
import de.julianostarek.motif.player.applemusic.isAppleMusicInstalled
import de.julianostarek.motif.player.spotify.isSpotifyInstalled
import de.julianostarek.motif.shared.BuildKonfig
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.*
import org.koin.dsl.module
import org.koin.ksp.generated.module

@Module
class AndroidModule {

    @Scope(PlayerViewModel::class)
    @Scoped
    fun appleMusicAuthentication(
        context: Context,
        externalScope: CoroutineScope
    ): AppleMusicAuthentication {
        return AppleMusicAuthentication(
            context,
            AppleMusicDeveloperToken(BuildKonfig.APPLE_DEVELOPER_TOKEN),
            externalScope
        )
    }

    @Single
    fun playerServiceAvailabilityInfo(
        context: Context,
        backendAuthRepository: BackendAuthRepository
    ): PlayerServiceAvailabilityInfo {
        return PlayerServiceAvailabilityInfo {
            listOf(
                PlayerServiceAvailabilityInfo.ServiceStatus(
                    service = PlayerService.APPLE_MUSIC,
                    isInstalled = isAppleMusicInstalled(context),
                    isLinked = backendAuthRepository.getServiceAuth(Service.AppleMusic) != null
                ),
                PlayerServiceAvailabilityInfo.ServiceStatus(
                    service = PlayerService.SPOTIFY,
                    isInstalled = isSpotifyInstalled(context),
                    isLinked = backendAuthRepository.getServiceAuth(Service.Spotify) != null
                )
            )
        }
    }

    @Single
    fun driverFactory(
        context: Context
    ): DriverFactory {
        return DriverFactory(context)
    }

    @OptIn(ExperimentalSettingsApi::class)
    @Named("LoginSettings")
    @Single
    fun loginSettings(
        context: Context
    ): ObservableSettings {
        val preferences = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        return AndroidSettings(preferences)
    }
}

fun androidStartKoin(context: Context) {
    val androidModule = module {
        single { context }
        includes(AndroidModule().module)
    }

    sharedStartKoin(platformModule = androidModule)
}