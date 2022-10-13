package de.julianostarek.motif.createmotif.datasource

import de.julianostarek.motif.createmotif.model.CreateMotif
import de.julianostarek.motif.createmotif.model.DetailedMotif

interface MotifDataSource {

    suspend fun createMotif(create: CreateMotif): DetailedMotif
}