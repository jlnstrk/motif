package de.julianostarek.motif.ui.listen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.julianostarek.motif.player.FrontendState

@Composable
fun Listen(
    viewModel: AndroidListenViewModel = viewModel()
) {
    Surface(Modifier.fillMaxSize()) {
        val context = LocalContext.current
        Column {
            FeedAppBar(Color.Black)
            Image(viewModel)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.connectAndPlay(context) }) {
                Text("Play!")
            }
            TextButton(onClick = { viewModel.connect(context) }) {
                Text("Connect!")
            }

            val frontendState = viewModel.frontendState.collectAsState()
            Text(if (frontendState.value is FrontendState.Connected) "Connected" else "Not Connected")
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
fun Image(viewModel: AndroidListenViewModel) {
    val image = viewModel.trackImage.collectAsState()
    AsyncImage(
        ImageRequest.Builder(LocalContext.current)
        .data(image.value)
        .crossfade(true)
        .build(), contentDescription = null, modifier = Modifier.size(256.dp).clip(RoundedCornerShape(8.dp)))
}