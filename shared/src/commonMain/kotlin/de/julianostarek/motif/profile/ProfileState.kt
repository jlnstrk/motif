package de.julianostarek.motif.profile

import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.domain.ProfileReference

sealed class ProfileState {
    abstract val reference: ProfileReference?

    data class NotLoaded(
        override val reference: ProfileReference?
    ) : ProfileState()

    data class Loading(
        override val reference: ProfileReference?
    ) : ProfileState()

    data class Loaded(
        override val reference: ProfileReference?,
        val profile: Profile.Detail
    ) : ProfileState()

    data class NotFound(
        override val reference: ProfileReference?,
    ) : ProfileState()
}