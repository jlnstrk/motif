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

package de.julianostarek.motif.detail

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.create.MotifRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class MotifDetailViewModel(private val motifId: Int) : SharedViewModel(), KoinComponent {
    private val repository: MotifRepository by inject { parametersOf(viewModelScope) }

    private val _state: MutableStateFlow<MotifDetailState> = MutableStateFlow(MotifDetailState.NotFound)
    val state: StateFlow<MotifDetailState> get() = _state

    init {
        viewModelScope.launch {
            repository.motifDetail(motifId)
                .onStart { _state.emit(MotifDetailState.Loading) }
                .map { motif -> MotifDetailState.Data(motif) }
                .collect(_state)
        }
    }
}