package de.julianostarek.motif.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import de.julianostarek.motif.AndroidPlayerConnector
import de.julianostarek.motif.player.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.get

class AndroidPlayerViewModel : ViewModel() {
    val shared: PlayerViewModel = PlayerViewModel()
    private val _trackImage: MutableStateFlow<Drawable?> = MutableStateFlow(null)
    val trackImage: StateFlow<Drawable?> get() = _trackImage

    fun playerConnectIntent(): Flow<Intent> = shared.get<AndroidPlayerConnector>().playerConnectIntent

    init {
        viewModelScope.launch {
            shared.frontendState
                .map { (it as? FrontendState.Connected.Playback)?.track }
                .distinctUntilChanged()
                .mapLatest { track -> track?.let { shared.playerOrNull()?.trackImage(track, 500) } }
                .collectLatest { image ->
                    val drawable: Drawable? = when (image) {
                        is AndroidTrackImage.Bitmap -> BitmapDrawable(image.bitmap)
                        is AndroidTrackImage.Url -> {
                            val context = shared.get<Context>()
                            val request = ImageRequest.Builder(context)
                                .data(image.url)
                                .build()
                            context.imageLoader.execute(request).drawable
                        }

                        null -> null
                    }
                    _trackImage.value = drawable
                }
        }

        viewModelScope.launch {
            trackImage.collectLatest {
                println("Bitmap: $it")
            }
        }
    }

    override fun onCleared() {
        shared.clear()
    }
}