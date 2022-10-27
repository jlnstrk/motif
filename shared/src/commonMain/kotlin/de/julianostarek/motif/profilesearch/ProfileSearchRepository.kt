package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.feed.domain.Profile

interface ProfileSearchRepository {
    suspend fun searchProfiles(query: String): List<Profile>
}