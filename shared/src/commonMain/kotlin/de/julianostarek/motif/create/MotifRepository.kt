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
import com.kuuurt.paging.multiplatform.PagingData
import de.julianostarek.motif.dto.MotifCreateDto
import de.julianostarek.motif.domain.Motif
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface MotifRepository {
    suspend fun createMotif(dto: MotifCreateDto): Motif.Detail
    suspend fun motifDetail(motifId: Int): Flow<Motif.Detail>
    fun motifsByProfile(profileId: String): Pager<*, Motif.Simple>
}