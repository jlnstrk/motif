package de.julianostarek.motif.feed

import de.julianostarek.motif.SharedViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

open class FeedViewModel : SharedViewModel(), KoinComponent {
    private val repository: FeedRepository by inject { parametersOf(viewModelScope) }

    private val _state: MutableStateFlow<FeedState> = MutableStateFlow(FeedState.NotLoading)
    val state: StateFlow<FeedState> get() = _state

    private var feedJob: Job? = null

    init {
        refreshFeed()
    }

    fun refreshFeed() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            repository.myFeed()
                .onStart { _state.value = FeedState.Loading }
                .collect {
                    _state.value = FeedState.Data(it)
                }
        }
    }
}