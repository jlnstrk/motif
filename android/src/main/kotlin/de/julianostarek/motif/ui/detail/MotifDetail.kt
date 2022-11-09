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

package de.julianostarek.motif.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.julianostarek.motif.detail.MotifDetailState
import de.julianostarek.motif.domain.Metadata
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.ui.player.AndroidPlayerViewModel
import de.julianostarek.motif.util.formatRelative

@Composable
fun MotifDetail(
    motifId: Int,
    playerViewModel: AndroidPlayerViewModel,
    viewModel: AndroidMotifDetailViewModel = viewModel { AndroidMotifDetailViewModel(motifId) }
) {
    val state = viewModel.shared.state.collectAsState().value
    Box(contentAlignment = Alignment.Center) {
        if (state is MotifDetailState.Data) {
            Column {
                CreatorRow(state.motif) {
                    // nop
                }
                Spacer(Modifier.fillMaxHeight())
            }
            CoverWithMetadata(state.motif.metadata) {
                playerViewModel.shared.play(state.motif)
            }
        }
    }
}

@Composable
private fun CreatorRow(
    motif: Motif.Detail,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            motif.creator.photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column {
            Text(motif.creator.username)
            Text(motif.createdAt.formatRelative())
        }
    }
}

@Composable
private fun CoverWithMetadata(
    metadata: Metadata?,
    onClick: () -> Unit
) {
    Column {
        AsyncImage(
            metadata?.coverArtUrl,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Text(metadata?.name ?: "Unknown track")
        Text(metadata?.name ?: "Unknown artist")
        Button(onClick = onClick) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        }
    }
}