package de.julianostarek.motif.feed

import de.julianostarek.motif.feed.datasource.FeedLocalDataSource
import de.julianostarek.motif.feed.datasource.FeedRemoteDataSource
import de.julianostarek.motif.feed.domain.Profile
import de.julianostarek.motif.feed.domain.Motif
import de.julianostarek.motif.feed.domain.ProfileWithMotifs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Single
class FeedRepositoryImpl(
    private val local: FeedLocalDataSource,
    private val remote: FeedRemoteDataSource,
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
                            remote.motifCreated.collect { data ->
                                local.insertMotif(data)
                            }
                        }
                        launch {
                            remote.motifDeleted.collect { data ->
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
                        local.dumpMotifsFeed(it)
                    }
                }
                addListener()
            }
            .onCompletion { removeListener() }
            .map { dtos ->
                dtos.groupBy { it.creatorId }
                    .map { group ->
                        val first = group.value.first()
                        val creator = Profile.Simple(
                            id = first.creatorId,
                            username = first.creatorUsername,
                            displayName = first.creatorDisplayName,
                            photoUrl = first.creatorPhotoUrl
                        )
                        ProfileWithMotifs(
                            profile = creator,
                            motifs = group.value.map {
                                Motif.Simple(
                                    id = it.id,
                                    isrc = it.isrc,
                                    offset = it.offset,
                                    listened = it.listened,
                                    createdAt = it.createdAt,
                                    creator = creator
                                )
                            }
                        )
                    }
            }
    }
}