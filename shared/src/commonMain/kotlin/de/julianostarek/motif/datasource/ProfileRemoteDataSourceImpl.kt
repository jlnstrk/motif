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

package de.julianostarek.motif.datasource

import com.apollographql.apollo3.api.Optional
import com.kuuurt.paging.multiplatform.PagingResult
import de.julianostarek.motif.client.*
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.graphql.toDetail
import de.julianostarek.motif.graphql.toSimple
import de.julianostarek.motif.graphql.toUpdate
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
                .execute().dataAssertNoErrors.profileMe.toDetail()
            emit(initial)

            val updates = backendClient.apollo.subscription(ProfileMeUpdatedSubscription())
                .toFlow()
                .map { response -> response.dataAssertNoErrors.profileMe.toDetail() }
            emitAll(updates)
        }
    }

    override fun profile(id: String): Flow<Profile.Detail?> {
        return backendClient.apollo.query(ProfileByIdQuery(id))
            .toFlow()
            .map { response -> response.dataAssertNoErrors.profileById?.toDetail() }
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return backendClient.apollo.query(ProfileIsUsernameAvailableQuery(username))
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

    override suspend fun searchProfiles(query: String, key: String?, count: Int): PagingResult<String, Profile> {
        val query = ProfileSearchQuery(
            query = query,
            first = count,
            after = Optional.presentIfNotNull(key)
        )
        val data = backendClient.apollo.query(query)
            .execute()
            .dataAssertNoErrors
            .profileSearch
        val page = data.nodes.map { profile -> profile.toSimple() }
        return PagingResult(
            items = page,
            currentKey = key ?: "",
            prevKey = { null },
            nextKey = { data.pageInfo.endCursor }
        )
    }
}