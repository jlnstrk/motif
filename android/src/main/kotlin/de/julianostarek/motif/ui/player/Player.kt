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

package de.julianostarek.motif.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.julianostarek.motif.R
import de.julianostarek.motif.player.RemoteState
import de.julianostarek.motif.player.PlayerService

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayerBarLayout(
    viewModel: AndroidPlayerViewModel,
    content: @Composable () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.Expanded }
    )
    ModalBottomSheetLayout(
        sheetContent = { PlayerChooser(viewModel) },
        sheetState = bottomSheetState,
        scrimColor = Color.Black.copy(alpha = 0.5F),
    ) {
        Box {
            content()
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                PlayerBar(
                    viewModel,
                    bottomSheetState = bottomSheetState,
                    modifier = Modifier.padding(horizontal = 8.dp)
                        .padding(bottom = 56.dp)
                )
            }
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
    val state = viewModel.shared.remoteState.collectAsState().value
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Choose a player")
                },
                elevation = 0.dp
            )
        },
        modifier = Modifier.clip(MaterialTheme.shapes.large),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        ) {
            itemsIndexed(viewModel.shared.availableServices) { index, availability ->
                Row(
                    modifier = Modifier.height(72.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { viewModel.shared.selectPlayer(availability.service) }
                        .fillParentMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(availability.service.brandingIconRes),
                        contentDescription = null,
                    )
                    Column {
                        Text(
                            text = availability.service.brandingName,
                            style = MaterialTheme.typography.body2
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (availability.isInstalled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null
                            )
                            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                                Text(
                                    text = if (availability.isInstalled) "Available" else "Not Installed",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                    Spacer(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1F)
                    )

                    if ((state as? RemoteState.ConnectingOrConnected)?.service == availability.service) {
                        when (state) {
                            is RemoteState.Connecting -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }

                            is RemoteState.Connected -> {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            }
                        }
                    }
                }
                if (index < viewModel.shared.availableServices.lastIndex) {
                    Divider(modifier = Modifier.padding(start = 64.dp, end = 8.dp))
                }
            }
        }
    }
}

val PlayerService.brandingIconRes: Int
    get() = when (this) {
        PlayerService.APPLE_MUSIC -> R.drawable.ic_apple_music
        PlayerService.SPOTIFY -> R.drawable.ic_spotify
    }

val PlayerService.brandingName: String
    get() = when (this) {
        PlayerService.APPLE_MUSIC -> "Apple Music"
        PlayerService.SPOTIFY -> "Spotify"
    }