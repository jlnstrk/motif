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