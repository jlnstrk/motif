package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileSearchViewModel : SharedViewModel(), KoinComponent {
    private val repository: ProfileSearchRepository by inject()
    private val query: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _state: MutableStateFlow<ProfileSearchState> = MutableStateFlow(ProfileSearchState.NoQuery)
    val state: StateFlow<ProfileSearchState> get() = _state

    init {
        viewModelScope.launch {
            query
                .flatMapLatest<_, ProfileSearchState> { query ->
                    if (!query.isNullOrEmpty()) {
                        flow {
                            emit(ProfileSearchState.Loading)
                            // Debounce (since flatMap*Latest*)
                            // Not using dedicated operator since we want to emit 'Loading' always
                            delay(QUERY_DELAY_MS)
                            val results = repository.searchProfiles(query)
                            if (results.isEmpty()) {
                                emit(ProfileSearchState.NoResults)
                            } else {
                                emit(ProfileSearchState.Results(results))
                            }
                        }
                    } else flowOf(ProfileSearchState.NoQuery)
                }
                .collect(_state)
        }
    }

    suspend fun setQuery(query: String?) {
        this.query.emit(query)
    }

    companion object {
        private const val QUERY_DELAY_MS = 500L
    }
}