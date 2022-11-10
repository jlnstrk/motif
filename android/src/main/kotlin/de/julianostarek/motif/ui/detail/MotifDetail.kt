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

import android.animation.ArgbEvaluator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import de.julianostarek.motif.detail.MotifDetailState
import de.julianostarek.motif.domain.Motif
import de.julianostarek.motif.ui.player.AndroidPlayerViewModel
import de.julianostarek.motif.util.formatRelative
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotifDetail(
    motifId: Int,
    playerViewModel: AndroidPlayerViewModel,
    viewModel: AndroidMotifDetailViewModel = viewModel { AndroidMotifDetailViewModel(motifId) }
) {
    val state = viewModel.shared.state.collectAsState().value
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val evaluator = remember { ArgbEvaluator() }
    Scaffold(
        topBar = {
            TopBar(
                viewModel = viewModel,
                state = state,
                scrollBehavior = scrollBehavior,
                onPlayClick = {
                    if (state is MotifDetailState.Data) {
                        playerViewModel.shared.play(state.motif)
                    }
                }
            ) { scrollPercent ->
                val scrolledColor = viewModel.themeColor.value ?: MaterialTheme.colorScheme.primaryContainer
                val color = Color(evaluator.evaluate(ceil(scrollPercent), Color.Transparent.toArgb(), scrolledColor.toArgb()) as Int)
                CenterAlignedTopAppBar(
                    title = {
                        if (state is MotifDetailState.Data) {
                            CreatorRow(state.motif) {
                                // nop
                            }
                        }
                    },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = color,
                        scrolledContainerColor = color
                    ),
                    modifier =Modifier
                        .shadow(elevation = if (scrollPercent > 0.01F) 8.dp else 0.dp)
                        .zIndex(1F)
                )
            }
        },
        backgroundColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Comments(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    viewModel: AndroidMotifDetailViewModel,
    state: MotifDetailState,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    onPlayClick: () -> Unit,
    content: @Composable (Float) -> Unit
) {
    val metadata = (state as? MotifDetailState.Data)?.motif?.metadata
    val isLoading = state is MotifDetailState.Loading
    val isLoaded = state is MotifDetailState.Data

    var imagePlaceholderVisible by remember { mutableStateOf(true) }

    val appBarDragModifier = if (!scrollBehavior.isPinned) {
        Modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                // scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffset + delta
            },
            onDragStopped = { velocity ->
                /*settleAppBar(
                    scrollBehavior.state,
                    velocity,
                    scrollBehavior.flingAnimationSpec,
                    scrollBehavior.snapAnimationSpec
                )*/
            }
        )
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.matchParentSize()) {
            val themeColor = viewModel.themeColor.collectAsState().value
            AnimatedVisibility(
                visible = themeColor != null,
                enter = fadeIn(tween(durationMillis = 750))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(listOf(themeColor!!, Color.Transparent))
                        )
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            content(scrollBehavior.state.heightOffset / scrollBehavior.state.heightOffsetLimit)

            var containerHeight: Float? by remember { mutableStateOf(null) }
            containerHeight?.let { height ->
                SideEffect {
                    if (scrollBehavior.state.heightOffsetLimit != height) {
                        scrollBehavior.state.heightOffsetLimit = height
                    }
                }
            }
            val heightModifier = containerHeight?.let {
                val height = ((-it + (scrollBehavior?.state?.heightOffset
                    ?: 0f)) / LocalDensity.current.density).dp
                println("setting height $height dp")
                Modifier.height(height)
            } ?: Modifier

            Box(modifier = Modifier.then(heightModifier)) {
                Column(modifier = appBarDragModifier
                    .wrapContentSize(align = Alignment.BottomCenter, unbounded = true)
                    .onSizeChanged { size ->
                        println("onSizeChanged $size")
                        containerHeight = min(-size.height.toFloat(), containerHeight ?: 0F)
                        println("onSizeChanged: CH $containerHeight")
                    }
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .shadow(16.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .placeholder(
                                visible = imagePlaceholderVisible,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(metadata?.coverArtUrl)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(256.dp),
                            onState = { state ->
                                if (state is AsyncImagePainter.State.Success) {
                                    viewModel.submitDrawableForThemeColor(state.result.drawable)
                                }
                                if ((state is AsyncImagePainter.State.Success || state is AsyncImagePainter.State.Error)
                                    && imagePlaceholderVisible
                                ) {
                                    imagePlaceholderVisible = false
                                }
                            },
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = metadata?.name ?: "",
                        modifier = Modifier
                            .defaultMinSize(minWidth = 128.dp)
                            .placeholder(
                                visible = state !is MotifDetailState.Data,
                                highlight = if (isLoaded) PlaceholderHighlight.shimmer() else null
                            )
                    )
                    Text(
                        text = metadata?.artist ?: "",
                        modifier = Modifier
                            .defaultMinSize(minWidth = 96.dp)
                            .placeholder(
                                visible = state !is MotifDetailState.Data,
                                highlight = if (isLoaded) PlaceholderHighlight.shimmer() else null
                            )
                    )
                    Button(onClick = onPlayClick) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorRow(
    motif: Motif.Detail,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            motif.creator.photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                text = motif.creator.username,
                style = MaterialTheme.typography.titleMedium
            )
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Text(
                    text = motif.createdAt.formatRelative(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CoverWithMetadata(
    viewModel: AndroidMotifDetailViewModel,
    state: MotifDetailState,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {

    }
}

@Composable
private fun Comments(
    viewModel: AndroidMotifDetailViewModel
) {
    LazyColumn {
        items(List(50) { "Lorem ipsum dolor sit amet" }) {
            Text(text = it, modifier = Modifier.fillMaxWidth().height(56.dp))
        }
    }
}