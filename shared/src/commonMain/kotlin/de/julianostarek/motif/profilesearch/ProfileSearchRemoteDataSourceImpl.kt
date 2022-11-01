package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.client.BackendClient
import de.julianostarek.motif.client.ProfileSearchQuery
import de.julianostarek.motif.dto.ProfileDto
import org.koin.core.annotation.Single

@Single
class ProfileSearchRemoteDataSourceImpl(
    private val backend: BackendClient
) : ProfileSearchRemoteDataSource {
    override suspend fun searchProfiles(query: String): List<ProfileDto> {
        return backend.apollo.query(ProfileSearchQuery(query))
            .execute()
            .dataAssertNoErrors
            .profileSearch
            .map { profile -> profile.toDto() }
    }

    private fun ProfileSearchQuery.ProfileSearch.toDto(): ProfileDto {
        return ProfileDto(
            displayName = displayName,
            id = id,
            photoUrl = photoUrl,
            username = username,
        )
    }
}