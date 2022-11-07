package de.julianostarek.motif.player.matching

import kotlinx.datetime.Instant

public interface MatchingCredentialsProvider {
    public suspend fun appleCredentials(): MatchingCredentials.AppleMusicCredentials?
    public suspend fun spotifyCredentials(): MatchingCredentials.SpotifyCredentials?
}

public sealed interface MatchingCredentials {
    public data class AppleMusicCredentials(
        public val developerToken: String,
        public val musicUserToken: String? = null
    ) : MatchingCredentials

    public data class SpotifyCredentials(
        public val clientId: String,
        // public val clientSecret: String,
        public val accessToken: String,
        public val expires: Instant
    )
}