package de.julianostarek.motif.createmotif.datasource

import de.julianostarek.motif.backend.BackendClient
import org.koin.core.annotation.Single

@Single
class RemoteMotifDataSource(
    private val backend: BackendClient
) {


}