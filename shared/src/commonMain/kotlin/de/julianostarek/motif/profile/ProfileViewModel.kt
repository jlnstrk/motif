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

package de.julianostarek.motif.profile

import com.kuuurt.paging.multiplatform.PagingData
import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.domain.ProfileReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ProfileViewModel(
    private val reference: ProfileReference?
) : SharedViewModel(), KoinComponent {
    private val repository: ProfileRepository by inject { parametersOf(viewModelScope) }

    private var refreshJob: Job? = null

    private val profile: MutableSharedFlow<Profile.Detail?> = MutableSharedFlow()
    private val motifs: MutableSharedFlow<PagingData<Motif.Simple>> = MutableSharedFlow()
    private val _state: MutableStateFlow<ProfileState> = MutableStateFlow(ProfileState.NotLoaded(reference))
    val state: StateFlow<ProfileState> get() = _state

    val isMyProfile: Boolean get() = reference == null

    init {
        viewModelScope.launch {
            combine(profile, motifs) { profile, motifs ->
                if (profile != null) {
                    ProfileState.Loaded(reference, profile, motifs)
                } else {
                    ProfileState.NotFound(reference)
                }
            }
                .collect(_state)
        }

        refresh()
    }

    suspend fun refreshSuspend() {
        withContext(viewModelScope.coroutineContext) {
            refresh()
            state.first { it is ProfileState.Loading }
            state.first { it !is ProfileState.Loading }
        }
    }

    fun refresh(): Job {
        refreshJob?.cancel()
        refreshJob = SupervisorJob()
        viewModelScope.launch(refreshJob!!) {
            (if (reference != null) repository.profile(reference.id) else repository.myProfile())
                .onStart {
                    _state.value = ProfileState.Loading(reference)
                }
                .collect(profile)
        }
        viewModelScope.launch(refreshJob!!) {
            (if (reference != null) repository.motifs(reference.id) else repository.myMotifs())
                .pagingData.collect(motifs)
        }
        return refreshJob!!
    }

    fun follow() = viewModelScope.launch {
        if (isMyProfile) return@launch
        if (repository.followProfile(reference!!.id)) {
            (state.value as? ProfileState.Loaded)?.profile?.let { profile ->
                this@ProfileViewModel.profile.emit(
                    profile.copy(
                        follows = true
                    )
                )
            }
        }
    }

    fun unfollow() = viewModelScope.launch {
        if (isMyProfile) return@launch
        if (repository.unfollowProfile(reference!!.id)) {
            (state.value as? ProfileState.Loaded)?.profile?.let { profile ->
                this@ProfileViewModel.profile.emit(
                    profile.copy(
                        follows = false
                    )
                )
            }
        }
    }
}