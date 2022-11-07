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

package de.julianostarek.motif.create

import com.kuuurt.paging.multiplatform.Pager
import com.kuuurt.paging.multiplatform.PagingConfig
import com.kuuurt.paging.multiplatform.PagingResult
import de.julianostarek.motif.datasource.MotifLocalDataSource
import de.julianostarek.motif.datasource.MotifRemoteDataSource
import de.julianostarek.motif.dto.MotifCreateDto
import de.julianostarek.motif.domain.Motif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.annotation.Single

@Single
class MotifRepositoryImpl(
    private val remote: MotifRemoteDataSource,
    private val local: MotifLocalDataSource,
    private val clientScope: CoroutineScope,
) : MotifRepository {
    override suspend fun createMotif(dto: MotifCreateDto): Motif.Detail {
        return remote.createMotif(dto)
    }

    override suspend fun motifDetail(motifId: Int): Flow<Motif.Detail> {
        return remote.motifDetail(motifId)
    }

    override fun motifsByProfile(profileId: String): Pager<String, Motif.Simple> {
        return Pager(
            clientScope,
            PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false),
            ""
        ) { key, pageSize ->
            remote.motifsByProfile(profileId, key = key, count = pageSize)
        }
    }
}