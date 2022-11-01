package de.julianostarek.motif.profile

import com.apollographql.apollo3.api.Optional
import de.julianostarek.motif.client.*
import de.julianostarek.motif.client.type.ProfileUpdate
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.profileedit.ProfileEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class ProfileRemoteDataSourceImpl(
    private val backendClient: BackendClient
) : ProfileRemoteDataSource {
    override fun myProfile(): Flow<Profile.Detail> {
        return flow {
            val initial = backendClient.apollo.query(ProfileMeQuery())
                .execute().dataAssertNoErrors.profileMe.toProfile()
            emit(initial)

            val updates = backendClient.apollo.subscription(ProfileMeUpdatedSubscription())
                .toFlow()
                .map { response -> response.dataAssertNoErrors.profileMe.toProfile() }
            emitAll(updates)
        }
    }

    override fun profile(id: String): Flow<Profile.Detail?> {
        return backendClient.apollo.query(ProfileByIdQuery(id))
            .toFlow()
            .map { response -> response.dataAssertNoErrors.profileById?.toProfile() }
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return backendClient.apollo.query(IsUsernameAvailableQuery(username))
            .execute().dataAssertNoErrors.profileIsUsernameAvailable
    }

    override suspend fun updateMyProfile(edit: ProfileEdit): Boolean {
        return !backendClient.apollo.mutation(ProfileMeUpdateMutation(edit.toUpdate()))
            .execute()
            .hasErrors()
    }

    override suspend fun followProfile(id: String): Boolean {
        return backendClient.apollo.mutation(ProfileFollowByIdMutation(id))
            .execute().dataAssertNoErrors.profileFollowById
    }

    override suspend fun unfollowProfile(id: String): Boolean {
        return backendClient.apollo.mutation(ProfileUnfollowByIdMutation(id))
            .execute().dataAssertNoErrors.profileUnfollowById
    }

    private fun ProfileMeUpdatedSubscription.ProfileMe.toProfile(): Profile.Detail {
        return Profile.Detail(
            displayName = displayName,
            id = id,
            username = username,
            photoUrl = photoUrl,
            biography = biography,
            follows = false,
            followersCount = followersCount,
            followingCount = followingCount
        )
    }

    private fun ProfileMeQuery.ProfileMe.toProfile(): Profile.Detail {
        return Profile.Detail(
            displayName = displayName,
            id = id,
            username = username,
            photoUrl = photoUrl,
            biography = biography,
            follows = false,
            followersCount = followersCount,
            followingCount = followingCount
        )
    }

    private fun ProfileByIdQuery.ProfileById.toProfile(): Profile.Detail {
        return Profile.Detail(
            displayName = displayName,
            id = id,
            username = username,
            photoUrl = photoUrl,
            biography = biography,
            follows = follows,
            followersCount = followersCount,
            followingCount = followingCount
        )
    }

    private fun ProfileEdit.toUpdate(): ProfileUpdate {
        return ProfileUpdate(
            displayName = Optional.presentIfNotNull(displayName),
            username = Optional.presentIfNotNull(username),
            photoUrl = Optional.absent(),
            biography = Optional.presentIfNotNull(biography)
        )
    }
}