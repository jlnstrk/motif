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

package de.julianostarek.motif.persist

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import de.julianostarek.motif.persist.adapter.InstantAdapter
import de.julianostarek.motif.persist.entity.CollectionEntity
import de.julianostarek.motif.persist.entity.MotifEntity

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): MotifDatabase {
    val driver = driverFactory.createDriver()
    val database = MotifDatabase(
        driver,
        collectionEntityAdapter = CollectionEntity.Adapter(
            createdAtAdapter = InstantAdapter,
            updatedAtAdapter = InstantAdapter
        ),
        motifEntityAdapter = MotifEntity.Adapter(
            createdAtAdapter = InstantAdapter,
            offsetAdapter = IntColumnAdapter
        )
    )
    return database
}