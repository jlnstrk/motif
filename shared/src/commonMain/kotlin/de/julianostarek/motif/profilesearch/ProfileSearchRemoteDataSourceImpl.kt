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

package de.julianostarek.motif.profilesearch

import de.julianostarek.motif.client.BackendClient
import de.julianostarek.motif.client.ProfileSearchQuery
import de.julianostarek.motif.dto.ProfileDto
import org.koin.core.annotation.Single

@Single
class ProfileSearchRemoteDataSourceImpl(
    private val backend: BackendClient
) : ProfileSearchRemoteDataSource {
    override suspend fun searchProfiles(query: String): List<ProfileDto> {
        return backend.apollo.query(ProfileSearchQuery(query))
            .execute()
            .dataAssertNoErrors
            .profileSearch
            .map { profile -> profile.toDto() }
    }

    private fun ProfileSearchQuery.ProfileSearch.toDto(): ProfileDto {
        return ProfileDto(
            displayName = displayName,
            id = id,
            photoUrl = photoUrl,
            username = username,
        )
    }
}