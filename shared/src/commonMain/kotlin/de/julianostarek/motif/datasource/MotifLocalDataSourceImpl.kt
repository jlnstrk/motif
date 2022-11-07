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

import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.persist.MotifDatabase
import de.julianostarek.motif.util.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class MotifLocalDataSourceImpl(
    private val database: MotifDatabase,
    private val externalScope: CoroutineScope
) : MotifLocalDataSource {
    override suspend fun saveMyFeed(motifsFeed: List<Motif>) {
        val ids = motifsFeed.map { it.id.toLong() }
        withContext(Dispatchers.Default) {
            database.transaction {
                database.motifQueries.deleteExceptIds(ids)
                motifsFeed.forEach { motif ->
                    database.motifQueries.upsert(motif.toEntity())
                    motif.creator?.toEntity()?.let(database.profileQueries::upsert)
                }
            }
        }
    }

    override suspend fun saveMotif(motif: Motif) {
        withContext(Dispatchers.Default) {
            database.transaction {
                database.motifQueries.upsert(motif.toEntity())
                motif.creator?.toEntity()?.let(database.profileQueries::upsert)
            }
        }
    }

    override suspend fun deleteMotif(motifId: Int) {
        withContext(Dispatchers.Default) {
            database.motifQueries.deleteById(motifId.toLong())
        }
    }
}