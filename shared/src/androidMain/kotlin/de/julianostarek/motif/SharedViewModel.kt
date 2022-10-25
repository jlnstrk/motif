package de.julianostarek.motif

import androidx.lifecycle.ViewModel as AndroidXViewModel
import androidx.lifecycle.viewModelScope as androidXViewModelScope
import kotlinx.coroutines.CoroutineScope

actual abstract class SharedViewModel : AndroidXViewModel() {
    protected actual val viewModelScope: CoroutineScope get() = androidXViewModelScope

    actual open fun clear() {
        onCleared()
    }
}