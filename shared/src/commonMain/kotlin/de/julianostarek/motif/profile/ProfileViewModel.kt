package de.julianostarek.motif.profile

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.domain.ProfileReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileViewModel(
    private val reference: ProfileReference?
) : SharedViewModel(), KoinComponent {
    private val repository: ProfileRepository by inject()

    private var profileJob: Job? = null

    private val profile: MutableSharedFlow<Profile.Detail?> = MutableSharedFlow()
    private val _state: MutableStateFlow<ProfileState> = MutableStateFlow(ProfileState.NotLoaded(reference))
    val state: StateFlow<ProfileState> get() = _state

    val isMyProfile: Boolean get() = reference == null

    init {
        viewModelScope.launch {
            profile.mapLatest { profile ->
                if (profile != null) {
                    ProfileState.Loaded(reference, profile)
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
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            (if (reference != null) repository.profile(reference.id) else repository.myProfile())
                .onStart {
                    _state.value = ProfileState.Loading(reference)
                }
                .collect(profile)
        }
        return profileJob!!
    }

    fun follow() = viewModelScope.launch {
        if (isMyProfile) return@launch
        if (repository.followProfile(reference!!.id)) {
            (state.value as? ProfileState.Loaded)?.profile?.let { profile ->
                this@ProfileViewModel.profile.emit(profile.copy(
                    follows = true
                ))
            }
        }
    }

    fun unfollow() = viewModelScope.launch {
        if (isMyProfile) return@launch
        if (repository.unfollowProfile(reference!!.id)) {
            (state.value as? ProfileState.Loaded)?.profile?.let { profile ->
                this@ProfileViewModel.profile.emit(profile.copy(
                    follows = false
                ))
            }
        }
    }
}