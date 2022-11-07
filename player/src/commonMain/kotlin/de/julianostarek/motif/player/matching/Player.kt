package de.julianostarek.motif.player.matching

import co.touchlab.kermit.Logger
import com.adamratzman.spotify.models.Token
import com.adamratzman.spotify.spotifyClientApi
import com.adamratzman.spotify.spotifyImplicitGrantApi
import de.julianostarek.motif.player.AppleMusicPlayer
import de.julianostarek.motif.player.Player
import de.julianostarek.motif.player.SpotifyPlayer
import de.julianostarek.motif.player.applemusic.MusicPlayerController
import de.julianostarek.motif.player.spotify.SpotifyRemote
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.serialization.json.Json

public suspend fun Player.playFromIsrc(credentialsProvider: MatchingCredentialsProvider, isrc: String): Boolean {
    Logger.i("Playing from ISRC: $isrc")
    return when (this) {
        is AppleMusicPlayer -> backing.playFromIsrc(credentialsProvider, isrc)
        is SpotifyPlayer -> backing.playFromIsrc(credentialsProvider, isrc)
    }
}

private suspend fun MusicPlayerController.playFromIsrc(
    credentialsProvider: MatchingCredentialsProvider,
    isrc: String
): Boolean {
    Logger.i("Playing from ISRC using Apple Music")
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                json = Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    val credentials = credentialsProvider.appleCredentials() ?: kotlin.run {
        Logger.i("Unable to match Apple Music track because of missing credentials")
        return false
    }
    val response = client.get(
        URLBuilder("https://api.music.apple.com/v1/catalog")
            .appendEncodedPathSegments("de")
            .appendPathSegments("songs")
            .build()
    ) {
        bearerAuth(credentials.developerToken)
        // header("Music-User-Token", credentials.musicUserToken)
        parameter("filter[isrc]", isrc)
    }

    val appleMusicResponse = response.body<AppleMusicResponse>()
    println(appleMusicResponse.data)
    val songId = appleMusicResponse.data.firstOrNull()?.id ?: kotlin.run {
        Logger.i("Couldn't match ISRC to any Apple Music track")
        return false
    }

    setQueue(listOf(songId), true)
    return true
}

private suspend fun SpotifyRemote.playFromIsrc(
    credentialsProvider: MatchingCredentialsProvider,
    isrc: String
): Boolean {
    Logger.i("Playing from ISRC using Spotify")
    val credentials = credentialsProvider.spotifyCredentials() ?: kotlin.run {
        Logger.i("Unable to match Spotify track because of missing credentials")
        return false
    }
    val spotifyApi = spotifyImplicitGrantApi(
        credentials.clientId,
        Token.from(
            accessToken = credentials.accessToken,
            refreshToken = null,
            scopes = emptyList(),
            expiresIn = Clock.System.now().periodUntil(credentials.expires, TimeZone.UTC).seconds
        )
    )

    val track = spotifyApi.search.searchTrack("isrc:$isrc", limit = 1).firstOrNull() ?: kotlin.run {
        Logger.i("Couldn't match ISRC to any Spotify track")
        return false
    }
    val uri = track.uri.uri
    playerApi.play(uri)
    return true
}