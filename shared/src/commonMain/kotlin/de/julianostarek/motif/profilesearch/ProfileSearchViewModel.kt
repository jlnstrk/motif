/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.julianostarek.motif.profilesearch

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import de.julianostarek.motif.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ProfileSearchViewModel : SharedViewModel(), KoinComponent {
    private val repository: ProfileSearchRepository by inject { parametersOf(viewModelScope) }
    private val query: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _state: MutableStateFlow<ProfileSearchState> = MutableStateFlow(ProfileSearchState.NoQuery)
    @NativeCoroutinesState
    val state: StateFlow<ProfileSearchState> get() = _state

    init {
        viewModelScope.launch {
            query.flatMapLatest<_, ProfileSearchState> { query ->
                if (!query.isNullOrEmpty()) {
                    flow {
                        emit(ProfileSearchState.Loading)
                        // Debounce (since flatMap*Latest*)
                        // Not using dedicated operator since we want to emit 'Loading' always
                        delay(QUERY_DELAY_MS)

                        val results = repository.searchProfiles(query).pagingData
                            .mapLatest { results ->
                                ProfileSearchState.Results(results)
                            }
                        emitAll(results)
                    }
                } else flowOf(ProfileSearchState.NoQuery)
            }
                .collect(_state)
        }
    }

    @NativeCoroutines
    suspend fun setQuery(query: String?) {
        this.query.emit(query)
    }

    companion object {
        private const val QUERY_DELAY_MS = 500L
    }
}