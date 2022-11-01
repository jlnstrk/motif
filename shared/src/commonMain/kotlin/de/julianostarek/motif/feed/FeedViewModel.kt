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

package de.julianostarek.motif.feed

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.domain.ProfileWithMotifs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

open class FeedViewModel : SharedViewModel(), KoinComponent {
    private val repository: FeedRepository by inject { parametersOf(viewModelScope) }

    private val _state: MutableStateFlow<FeedState> = MutableStateFlow(FeedState.NotLoading)
    val state: StateFlow<FeedState> get() = _state

    private var feedJob: Job? = null

    init {
        refreshFeed()
    }

    fun refreshFeed() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            repository.myFeed()
                .onStart { _state.value = FeedState.Loading }
                .collect { profiles ->
                    _state.value = FeedState.Data(
                        profilesGrid = profiles.sortedByDescending { it.motifs.first().createdAt }
                            .toSpiralHexagon()
                    )
                }
        }
    }
}
