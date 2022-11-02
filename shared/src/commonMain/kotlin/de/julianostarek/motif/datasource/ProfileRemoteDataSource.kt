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

import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.profileedit.ProfileEdit
import kotlinx.coroutines.flow.Flow

interface ProfileRemoteDataSource {
    fun myProfile(): Flow<Profile.Detail>

    fun profile(id: String): Flow<Profile.Detail?>

    suspend fun isUsernameAvailable(username: String): Boolean

    suspend fun updateMyProfile(edit: ProfileEdit): Boolean

    suspend fun followProfile(id: String): Boolean

    suspend fun unfollowProfile(id: String): Boolean

    suspend fun searchProfiles(query: String): List<Profile>
}