package de.julianostarek.motif.profile

import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.profileedit.ProfileEdit
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun myProfile(): Flow<Profile.Detail>

    fun profile(id: String): Flow<Profile.Detail?>

    suspend fun isUsernameAvailable(username: String): Boolean

    suspend fun updateMyProfile(edit: ProfileEdit): Boolean

    suspend fun followProfile(id: String): Boolean

    suspend fun unfollowProfile(id: String): Boolean
}