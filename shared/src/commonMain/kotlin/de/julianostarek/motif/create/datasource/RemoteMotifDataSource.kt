package de.julianostarek.motif.create.datasource

import de.julianostarek.motif.client.BackendClient
import org.koin.core.annotation.Single

@Single
class RemoteMotifDataSource(
    private val backend: BackendClient
) {


}