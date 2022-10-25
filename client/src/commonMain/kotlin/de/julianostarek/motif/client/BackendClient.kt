package de.julianostarek.motif.client

import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.jvm.JvmInline

@Single
@JvmInline
value class BackendClient(
    @Named("MotifClient")
    val apollo: ApolloClient
)