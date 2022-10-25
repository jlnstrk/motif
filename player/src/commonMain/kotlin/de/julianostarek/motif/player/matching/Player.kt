package de.julianostarek.motif.player.matching

import com.adamratzman.spotify.models.Token
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
    return when (this) {
        is AppleMusicPlayer -> backing.playFromIsrc(credentialsProvider, isrc)
        is SpotifyPlayer -> backing.playFromIsrc(credentialsProvider, isrc)
    }
}

private suspend fun MusicPlayerController.playFromIsrc(
    credentialsProvider: MatchingCredentialsProvider,
    isrc: String
): Boolean {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                json = Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    val credentials = credentialsProvider.appleCredentials() ?: return false
    val response = client.get(
        URLBuilder("https://api.music.apple.com/v1/catalog")
            .appendEncodedPathSegments("de")
            .appendPathSegments("songs")
            .build()
    ) {
        bearerAuth(credentials.developerToken)
        header("Music-User-Token", credentials.musicUserToken)
        parameter("filter[isrc]", isrc)
    }

    val appleMusicResponse = response.body<AppleMusicResponse>()
    val songId = appleMusicResponse.data.firstOrNull()?.id ?: return false

    setQueue(listOf(songId), true)
    return true
}

private suspend fun SpotifyRemote.playFromIsrc(
    credentialsProvider: MatchingCredentialsProvider,
    isrc: String
): Boolean {
    val credentials = credentialsProvider.spotifyCredentials() ?: return false
    val spotifyApi = spotifyImplicitGrantApi(
        null, Token.from(
            accessToken = credentials.accessToken,
            refreshToken = null,
            scopes = emptyList(),
            expiresIn = Clock.System.now().periodUntil(credentials.expires, TimeZone.UTC).seconds
        )
    )

    val track = spotifyApi.search.searchTrack("isrc:$isrc", limit = 1).firstOrNull() ?: return false
    val uri = track.uri.uri
    playerApi.play(uri)
    return true
}