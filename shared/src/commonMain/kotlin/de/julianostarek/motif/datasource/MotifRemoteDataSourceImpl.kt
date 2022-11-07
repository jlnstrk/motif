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
import de.julianostarek.motif.dto.MotifCreateDto
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.domain.ProfileWithMotifs
import de.julianostarek.motif.graphql.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.koin.core.annotation.Single

@Single
class MotifRemoteDataSourceImpl(
    private val backend: BackendClient,
    clientScope: CoroutineScope
) : MotifRemoteDataSource {
    private val motifCreated: Flow<Motif.Simple> = backend.apollo.subscription(MotifCreatedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifCreated.toSimple() }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())
    private val motifDeleted: Flow<Int> = backend.apollo.subscription(MotifDeletedSubscription())
        .toFlow()
        .map { it.dataAssertNoErrors.motifDeleted }
        .shareIn(clientScope, SharingStarted.WhileSubscribed())

    override fun motifCreated(): Flow<Motif.Simple> = motifCreated
    override fun motifDeleted(): Flow<Int> = motifDeleted

    override suspend fun feedProfiles(cursor: String?, count: Int): PagingResult<String, ProfileWithMotifs> {
        val query = FeedProfilesQuery(
            first = Optional.present(count),
            after = Optional.present(cursor)
        )
        val data = backend.apollo.query(query)
            .execute()
            .dataAssertNoErrors.feedProfiles
        val items = data.nodes.map { it.toProfileWithMotifs() }
        return PagingResult(
            items = items,
            currentKey = cursor ?: "",
            prevKey = { null },
            nextKey = { data.pageInfo.endCursor }
        )
    }

    override suspend fun motifs(cursor: String?, count: Int): PagingResult<String, Motif.Simple> {
        val query = ProfileMeMotifsQuery(
            first = Optional.present(count),
            after = Optional.present(cursor)
        )
        val data = backend.apollo.query(query)
            .execute()
            .dataAssertNoErrors.profileMe.motifs
        val items = data.nodes.map { it.toSimpleMotif() }
        return PagingResult(
            items = items,
            currentKey = cursor ?: "",
            prevKey = { null },
            nextKey = { data.pageInfo.endCursor }
        )
    }

    override suspend fun motifsByProfile(
        profileId: String,
        key: String?,
        count: Int
    ): PagingResult<String, Motif.Simple> {
        val query = ProfileByIdMotifsQuery(
            profileId = profileId,
            first = Optional.present(count),
            after = Optional.present(key)
        )
        val data = backend.apollo.query(query)
            .execute()
            .dataAssertNoErrors.profileById!!.motifs
        val items = data.nodes.map { it.toSimpleMotif() }
        return PagingResult(
            items = items,
            currentKey = key ?: "",
            prevKey = { null },
            nextKey = { data.pageInfo.endCursor }
        )
    }

    override suspend fun createMotif(dto: MotifCreateDto): Motif.Detail {
        return backend.apollo.mutation(MotifCreateMutation(dto.toCreateMotif()))
            .execute()
            .dataAssertNoErrors
            .motifCreate
            .toDetail()
    }

    override suspend fun motifDetail(motifId: Int): Flow<Motif.Detail> {
        return backend.apollo.query(MotifDetailQuery(motifId))
            .toFlow()
            .map { it.dataAssertNoErrors.motifById.toDetail() }
    }
}