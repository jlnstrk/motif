package de.julianostarek.motif.player

import de.julianostarek.motif.client.auth.BackendAuthRepository
import de.julianostarek.motif.client.auth.Service
import de.julianostarek.motif.player.matching.MatchingCredentials
import de.julianostarek.motif.player.matching.MatchingCredentialsProvider
import de.julianostarek.motif.shared.BuildKonfig
import org.koin.core.annotation.Single

@Single
class BackendMatchingCredentialsProvider(
    private val authRepository: BackendAuthRepository
) : MatchingCredentialsProvider {
    override suspend fun appleCredentials(): MatchingCredentials.AppleMusicCredentials? {
        return authRepository.getServiceAuthOptionalRefresh(Service.AppleMusic)?.let { token ->
            MatchingCredentials.AppleMusicCredentials(
                developerToken = BuildKonfig.APPLE_DEVELOPER_TOKEN,
                musicUserToken = token.accessToken
            )
        }
    }

    override suspend fun spotifyCredentials(): MatchingCredentials.SpotifyCredentials? {
        return authRepository.getServiceAuthOptionalRefresh(Service.Spotify)?.let { token ->
            MatchingCredentials.SpotifyCredentials(
                accessToken = token.accessToken,
                expires = token.accessTokenExpires!!
            )
        }
    }
}