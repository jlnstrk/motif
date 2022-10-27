package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.dto.ProfileDto
import de.julianostarek.motif.feed.domain.Profile
import org.koin.core.annotation.Single

@Single
class ProfileSearchRepositoryImpl(
    private val remoteDataSource: ProfileSearchRemoteDataSource
) : ProfileSearchRepository {
    override suspend fun searchProfiles(query: String): List<Profile> {
        return remoteDataSource.searchProfiles(query)
            .map { dto -> dto.toDomain() }
    }

    private fun ProfileDto.toDomain(): Profile.Simple {
        return Profile.Simple(
            displayName = displayName,
            id = id,
            photoUrl = photoUrl,
            username = username,
        )
    }
}