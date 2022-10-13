package de.julianostarek.motif

import kotlinx.coroutines.CoroutineScope

expect abstract class SharedViewModel() {
    protected val viewModelScope: CoroutineScope
}