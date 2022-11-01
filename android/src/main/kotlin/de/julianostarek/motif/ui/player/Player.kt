package de.julianostarek.motif.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.julianostarek.motif.R
import de.julianostarek.motif.player.FrontendState
import de.julianostarek.motif.player.PlayerService

@Composable
fun Player(
    viewModel: AndroidPlayerViewModel
) {
    Surface(Modifier.fillMaxSize()) {
        val context = LocalContext.current
        Column {
            FeedAppBar(Color.Black)
            TrackImage(viewModel)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.shared.disconnect() }) {
                Text("Disconnect")
            }
            TextButton(onClick = { viewModel.shared.selectPlayer(PlayerService.APPLE_MUSIC) }) {
                Text("Connect to Apple Music")
            }
            TextButton(onClick = { viewModel.shared.selectPlayer(PlayerService.SPOTIFY) }) {
                Text("Connect to Spotify")
            }

            val frontendState = viewModel.shared.frontendState.collectAsState()
            Text(
                when (val state = frontendState.value) {
                    is FrontendState.Connected.NoPlayback -> "Connected: ${state.service}. (No Playback)"
                    is FrontendState.Connected.Playback -> "Connected: ${state.service}. (${state.track.title})"
                    FrontendState.Disconnected -> "Disconnected"
                    is FrontendState.Connecting -> "Connecting: ${state.service}"
                }
            )
        }
    }
}

@Composable
fun FeedAppBar(
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("Motif") },
        backgroundColor = backgroundColor,
        actions = {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                IconButton(
                    onClick = { /* TODO: Open search */ }
                ) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                }
                IconButton(
                    onClick = { /* TODO: Open account? */ }
                ) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun TrackImage(viewModel: AndroidPlayerViewModel) {
    val image = viewModel.trackImage.collectAsState()
    AsyncImage(
        ImageRequest.Builder(LocalContext.current)
            .data(image.value)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = Modifier.size(256.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun PlayerChooser(
    viewModel: AndroidPlayerViewModel
) {
    val state = viewModel.shared.frontendState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(viewModel.shared.availableServices) { availability ->
            Row {
                Image(painter = painterResource(availability.service.brandingIconRes), contentDescription = null)
                Column {
                    Text(availability.service.brandingName)
                    Row {
                        Icon(
                            imageVector = if (availability.isInstalled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null
                        )
                        Text(if (availability.isInstalled) "Available" else "Not Installed")
                    }
                }
            }
        }
    }
}

private val PlayerService.brandingIconRes: Int
    get() = when (this) {
        PlayerService.APPLE_MUSIC -> R.drawable.ic_apple_music
        PlayerService.SPOTIFY -> R.drawable.ic_spotify
    }

private val PlayerService.brandingName: String
    get() = when (this) {
        PlayerService.APPLE_MUSIC -> "Apple Music"
        PlayerService.SPOTIFY -> "Spotify"
    }