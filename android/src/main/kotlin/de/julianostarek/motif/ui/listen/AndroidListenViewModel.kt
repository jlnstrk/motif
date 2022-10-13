package de.julianostarek.motif.ui.listen

import android.content.Context
import android.graphics.Bitmap
import de.julianostarek.motif.player.spotify.ImageDimension
import de.julianostarek.motif.player.FrontendState
import de.julianostarek.motif.player.PlayerViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AndroidListenViewModel : PlayerViewModel() {
    private val _trackImage: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val trackImage: StateFlow<Bitmap?> get() = _trackImage

    init {
        viewModelScope.launch {
            frontendState
                .map { (it as? FrontendState.Connected.Playback)?.track }
                .distinctUntilChanged { old, new -> old?.uri == new?.uri }
                .mapLatest { track -> track?.let { spotifyRemote?.imagesApi?.getImage(track, ImageDimension.LARGE) } }
                .collectLatest { _trackImage.value = it }
        }

        viewModelScope.launch {
            trackImage.collectLatest {
                println("Bitmap: $it")
            }
        }
    }

    fun connectAndPlay(context: Context) = viewModelScope.launch {
        spotifyConnector.connectAndPlay(context, "spotify:track:4WvU1mDTPDRJ349kqLK9OH")
    }

    fun connect(context: Context) = viewModelScope.launch {
        spotifyConnector.connect(context)
    }
}