package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.domain.Profile

sealed class ProfileSearchState {
    object NoQuery : ProfileSearchState()
    object Loading : ProfileSearchState()
    data class Results(val results: List<Profile>) : ProfileSearchState()
    object NoResults : ProfileSearchState()
}