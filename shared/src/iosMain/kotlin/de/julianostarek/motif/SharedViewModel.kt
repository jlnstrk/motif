package de.julianostarek.motif

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

actual abstract class SharedViewModel {
    actual val viewModelScope: CoroutineScope = MainScope()

    actual open fun clear() {
        viewModelScope.coroutineContext.cancel()
    }
}