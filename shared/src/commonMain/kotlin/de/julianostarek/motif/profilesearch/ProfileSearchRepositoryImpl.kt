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

import com.kuuurt.paging.multiplatform.Pager
import com.kuuurt.paging.multiplatform.PagingConfig
import com.kuuurt.paging.multiplatform.PagingResult
import de.julianostarek.motif.datasource.ProfileLocalDataSource
import de.julianostarek.motif.datasource.ProfileRemoteDataSource
import de.julianostarek.motif.domain.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Single
class ProfileSearchRepositoryImpl(
    private val remote: ProfileRemoteDataSource,
    private val local: ProfileLocalDataSource,
    @InjectedParam private val clientScope: CoroutineScope,
) : ProfileSearchRepository {
    override suspend fun searchProfiles(query: String): Pager<String, Profile> {
        return Pager(
            clientScope,
            config = PagingConfig(pageSize = SEARCH_PAGE_SIZE),
            initialKey = ""
        ) { key, count ->
            remote.searchProfiles(query, key.takeIf(String::isNotEmpty), count)
        }
    }

    companion object {
        const val SEARCH_PAGE_SIZE = 10
    }
}