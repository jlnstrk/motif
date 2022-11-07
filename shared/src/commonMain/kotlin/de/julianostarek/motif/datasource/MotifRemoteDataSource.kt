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

import com.kuuurt.paging.multiplatform.PagingResult
import de.julianostarek.motif.dto.MotifCreateDto
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.ProfileWithMotifs
import kotlinx.coroutines.flow.Flow

interface MotifRemoteDataSource {
    fun motifCreated(): Flow<Motif.Simple>
    fun motifDeleted(): Flow<Int>

    suspend fun feedProfiles(cursor: String?, count: Int): PagingResult<String, ProfileWithMotifs>
    suspend fun motifs(cursor: String?, count: Int): PagingResult<String, Motif.Simple>
    suspend fun motifsByProfile(profileId: String, key: String?, count: Int): PagingResult<String, Motif.Simple>

    suspend fun createMotif(dto: MotifCreateDto): Motif.Detail
    suspend fun motifDetail(motifId: Int): Flow<Motif.Detail>
}