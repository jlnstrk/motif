package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.dto.ProfileDto


interface ProfileSearchRemoteDataSource {
    suspend fun searchProfiles(query: String): List<ProfileDto>
}