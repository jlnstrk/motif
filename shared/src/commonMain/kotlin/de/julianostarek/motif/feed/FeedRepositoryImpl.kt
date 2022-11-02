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

package de.julianostarek.motif.feed

import de.julianostarek.motif.datasource.MotifLocalDataSource
import de.julianostarek.motif.datasource.MotifRemoteDataSource
import de.julianostarek.motif.domain.ProfileWithMotifs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Single
class FeedRepositoryImpl(
    private val local: MotifLocalDataSource,
    private val remote: MotifRemoteDataSource,
    @InjectedParam private val clientScope: CoroutineScope
) : FeedRepository {
    private val listeners = MutableStateFlow(0)
    private val _isFeedRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isFeedRefreshing: StateFlow<Boolean> get() = _isFeedRefreshing

    init {
        clientScope.launch {
            var currentJob: Job? = null
            listeners.collect { listeners ->
                if (listeners >= 1 && currentJob?.isActive != true) {
                    currentJob = launch {
                        launch {
                            remote.motifCreated().collect { data ->
                                local.saveMotif(data)
                            }
                        }
                        launch {
                            remote.motifDeleted().collect { data ->
                                local.deleteMotif(data)
                            }
                        }
                    }
                } else {
                    currentJob?.cancel()
                }
            }
        }
    }

    private fun addListener() = listeners.getAndUpdate { it + 1 }

    private fun removeListener() = listeners.getAndUpdate { it - 1 }

    override fun myFeed(): Flow<List<ProfileWithMotifs>> {
        return local.motifsFeed()
            .onStart {
                clientScope.launch {
                    remote.motifsFeed().collect {
                        local.saveMyFeed(it)
                    }
                }
                addListener()
            }
            .onCompletion { removeListener() }
            .map { motifs ->
                motifs.groupBy { it.creator.id }
                    .map { group ->
                        val first = group.value.first()
                        val creator = first.creator
                        ProfileWithMotifs(
                            profile = creator,
                            motifs = group.value
                        )
                    }
            }
    }
}