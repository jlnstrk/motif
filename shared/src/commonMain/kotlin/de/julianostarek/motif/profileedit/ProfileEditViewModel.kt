package de.julianostarek.motif.profileedit

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.profile.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileEditViewModel : SharedViewModel(), KoinComponent {
    private val repository: ProfileRepository by inject()

    private val profile: MutableStateFlow<Profile.Detail?> = MutableStateFlow(null)

    private val _userInput: MutableStateFlow<ProfileEdit?> = MutableStateFlow(null)
    val userInput: StateFlow<ProfileEdit?> get() = _userInput

    private val changedFields: StateFlow<ProfileEdit?> = combine(profile, _userInput) { profile, input ->
        if (input == null || profile == null) {
            return@combine null
        }
        ProfileEdit(
            displayName = input.displayName.takeIf { it != profile.displayName },
            username = input.username.takeIf { it != profile.username },
            biography = input.biography.takeIf { it.orEmpty() != profile.biography.orEmpty() }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileEdit.NO_CHANGES)

    private val _usernameAvailability: MutableStateFlow<ProfileUsernameAvailability> =
        MutableStateFlow(ProfileUsernameAvailability.IS_OWN)
    val usernameAvailability: StateFlow<ProfileUsernameAvailability> get() = _usernameAvailability

    private val submissionSignal: MutableSharedFlow<Unit> = MutableSharedFlow()
    private val rawSubmissionStatus: MutableStateFlow<ProfileEditSubmissionStatus> =
        MutableStateFlow(ProfileEditSubmissionStatus.SUBMITTED)
    val submissionStatus: StateFlow<ProfileEditSubmissionStatus> = rawSubmissionStatus.flatMapLatest { rawStatus ->
        flow {
            emit(rawStatus)

            if (rawStatus == ProfileEditSubmissionStatus.OUTSTANDING) {
                emitAll(submissionSignal.flatMapLatest {
                    flow {
                        changedFields.value?.let { edit ->

                            emit(ProfileEditSubmissionStatus.IN_PROGRESS)
                            if (repository.updateMyProfile(edit)) {
                                emit(ProfileEditSubmissionStatus.SUBMITTED)
                            } else {
                                emit(ProfileEditSubmissionStatus.FAILED)
                            }
                        }
                    }
                })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileEditSubmissionStatus.SUBMITTED)

    val canSubmit: StateFlow<Boolean> =
        combine(submissionStatus, usernameAvailability) { submissionStatus, usernameAvailability ->
            submissionStatus == ProfileEditSubmissionStatus.OUTSTANDING && usernameAvailability != ProfileUsernameAvailability.UNAVAILABLE
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    init {
        viewModelScope.launch {
            repository.myProfile().collect(profile)
        }

        viewModelScope.launch {
            val currentProfile = profile.filterNotNull().first()
            _userInput.value = ProfileEdit(
                displayName = currentProfile.displayName,
                username = currentProfile.username,
                biography = currentProfile.biography
            )
        }

        viewModelScope.launch {
            _userInput
                .map { edit -> edit?.username }
                .distinctUntilChanged()
                .flatMapLatest { username ->
                    if (username == null || username == profile.value?.username) {
                        return@flatMapLatest flowOf(ProfileUsernameAvailability.IS_OWN)
                    }
                    flow {
                        emit(ProfileUsernameAvailability.LOADING)
                        delay(USERNAME_CHECK_DEBOUNCE_MS)
                        val status = when (repository.isUsernameAvailable(username)) {
                            true -> ProfileUsernameAvailability.AVAILABLE
                            false -> ProfileUsernameAvailability.UNAVAILABLE
                        }
                        emit(status)
                    }
                }
                .collect(_usernameAvailability)
        }

        viewModelScope.launch {
            changedFields.mapLatest { input ->
                if (input != ProfileEdit.NO_CHANGES) {
                    ProfileEditSubmissionStatus.OUTSTANDING
                } else {
                    ProfileEditSubmissionStatus.SUBMITTED
                }
            }
                .collect(rawSubmissionStatus)
        }
    }

    fun setInput(input: ProfileEdit) {
        this._userInput.value = input
    }

    fun submit() = viewModelScope.launch {
        submissionSignal.emit(Unit)
    }

    companion object {
        private const val USERNAME_CHECK_DEBOUNCE_MS = 1000L
    }
}