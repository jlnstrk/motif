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

import app.cash.sqldelight.coroutines.asFlow
import de.julianostarek.motif.domain.Profile
import de.julianostarek.motif.persist.MotifDatabase
import de.julianostarek.motif.persist.entity.ProfileEntity
import de.julianostarek.motif.util.toEntity
import de.julianostarek.motif.util.toSimple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.withContext

class ProfileLocalDataSourceImpl(
    private val database: MotifDatabase
) : ProfileLocalDataSource {
    override suspend fun searchProfiles(query: String): List<Profile.Simple> {
        return withContext(Dispatchers.Default) {
            database.profileQueries.search(query)
                .executeAsList()
                .map(ProfileEntity::toSimple)
        }
    }

    override suspend fun saveProfiles(profiles: List<Profile>) {
        withContext(Dispatchers.Default) {
            database.transaction {
                profiles.map(Profile::toEntity)
                    .forEach(database.profileQueries::upsert)
            }
        }
    }
}