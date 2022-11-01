package de.julianostarek.motif.profile

import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.profileedit.ProfileEdit
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

// TODO: Cache logged in user's profile in DB
@Single
class ProfileRepositoryImpl(
    private val remote: ProfileRemoteDataSource
) : ProfileRepository {
    override fun myProfile(): Flow<Profile.Detail> {
        return remote.myProfile()
    }

    override fun profile(id: String): Flow<Profile.Detail?> {
        return remote.profile(id)
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return remote.isUsernameAvailable(username)
    }

    override suspend fun updateMyProfile(edit: ProfileEdit): Boolean {
        return remote.updateMyProfile(edit)
    }

    override suspend fun followProfile(id: String): Boolean {
        return remote.followProfile(id)
    }

    override suspend fun unfollowProfile(id: String): Boolean {
        return remote.unfollowProfile(id)
    }
}