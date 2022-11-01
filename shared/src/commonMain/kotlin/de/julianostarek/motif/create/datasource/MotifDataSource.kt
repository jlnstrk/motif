package de.julianostarek.motif.create.datasource

import de.julianostarek.motif.create.model.CreateMotif
import de.julianostarek.motif.create.model.DetailedMotif

interface MotifDataSource {

    suspend fun createMotif(create: CreateMotif): DetailedMotif
}