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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.julianostarek.motif.player.RemoteState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayerBar(
    viewModel: AndroidPlayerViewModel,
    bottomSheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier
) {
    var isPresentingControls by remember { mutableStateOf(false) }
    val state = viewModel.shared.remoteState.collectAsState().value

    Surface(
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp,
        tonalElevation = 4.dp,
        modifier = modifier.clickable(
            enabled = state is RemoteState.Connected,
            onClick = { isPresentingControls = !isPresentingControls }
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.height(56.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state is RemoteState.Connected) {
                    val imageModifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .fillMaxHeight()
                    val trackImage = viewModel.trackImage.collectAsState().value
                    if (trackImage != null) {
                        Image(
                            painter = rememberDrawablePainter(trackImage),
                            contentDescription = null,
                            modifier = imageModifier,
                        )
                    } else {
                        DefaultCoverArt(modifier = imageModifier)
                    }
                }

                MetadataText(state, modifier = Modifier.weight(1F))
                Spacer(Modifier)

                if (state is RemoteState.Connected) {
                    PlayPauseButton(viewModel, state)
                } else {
                    ConnectButton(bottomSheetState, state)
                }
            }
            if (isPresentingControls) {
                Column {
                    Divider(modifier = Modifier.padding(horizontal = 8.dp))
                    AdditionalControls(viewModel, bottomSheetState, state)
                }
            }
        }
    }

}

@Composable
private fun MetadataText(
    state: RemoteState,
    modifier: Modifier = Modifier
) {
    if (state is RemoteState.Connected) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = modifier) {
            Text(
                text = when (state) {
                    is RemoteState.Connected.Playback -> {
                        "${state.track.title} • ${state.track.artists.joinToString()}"
                    }

                    else -> "No Playback"
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(state.service.brandingIconRes),
                    contentDescription = null
                )

                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    Text(
                        text = state.service.brandingName,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    } else {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier
            ) {
                Icon(imageVector = Icons.Rounded.Warning, contentDescription = null)
                Text("Not Connected", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun Enabled(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.33F)) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun PlayPauseButton(
    viewModel: AndroidPlayerViewModel,
    state: RemoteState,
    modifier: Modifier = Modifier
) {
    if (state is RemoteState.Connected) {
        val playbackState = state as? RemoteState.Connected.Playback

        Enabled(playbackState != null) {
            IconButton(
                enabled = playbackState != null,
                onClick = {
                    if (playbackState?.isPaused != false) {
                        viewModel.shared.resume()
                    } else {
                        viewModel.shared.pause()
                    }
                },
                modifier = modifier
            ) {
                Icon(
                    imageVector = if (playbackState?.isPaused != false) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun OpenButton(
    viewModel: AndroidPlayerViewModel
) {
    TextButton(onClick = {}) {
        Text("Open")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ConnectButton(
    bottomSheetState: ModalBottomSheetState,
    state: RemoteState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    TextButton(
        onClick = { scope.launch { bottomSheetState.show() } },
        modifier = modifier
    ) {
        Text(if (state is RemoteState.Connected) "Switch" else "Connect")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AdditionalControls(
    viewModel: AndroidPlayerViewModel,
    bottomSheetState: ModalBottomSheetState,
    state: RemoteState,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        OpenButton(viewModel)
        ConnectButton(bottomSheetState, state)
    }
}