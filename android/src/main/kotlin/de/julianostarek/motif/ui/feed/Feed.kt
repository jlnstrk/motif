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

package de.julianostarek.motif.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import de.julianostarek.motif.feed.FeedState
import de.julianostarek.motif.domain.ProfileWithMotifs
import de.julianostarek.motif.ui.PrimaryDark
import de.julianostarek.motif.ui.PrimaryLight
import de.julianostarek.motif.ui.Screen
import de.julianostarek.motif.ui.player.AndroidPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Feed(
    playerViewModel: AndroidPlayerViewModel,
    feedViewModel: AndroidFeedViewModel = viewModel()
) {
    val feedState by feedViewModel.shared.state.collectAsState()
    val appBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(appBarState)

    Scaffold(
        topBar = {
            FeedAppBar(scrollBehavior)
        },
        containerColor = Color.Transparent
    ) { padding ->
        FeedContent(feedViewModel,
            modifier = Modifier.padding(padding),
            feedState = feedState,
            scrollBehavior = scrollBehavior,
            onProfileClick = { profileWithMotifs ->
                playerViewModel.shared.play(profileWithMotifs.motifs.first())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent)) {
        LargeTopAppBar(
            title = { Text("Feed", modifier = Modifier.zIndex(10F)) },
            actions = {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    IconButton(
                        onClick = { /* TODO: Open search */ }
                    ) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    }
                }
            },
            scrollBehavior = scrollBehavior,
            modifier = modifier.background(Brush.verticalGradient(listOf(PrimaryDark, Color.Transparent))),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    viewModel: AndroidFeedViewModel,
    feedState: FeedState,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    onProfileClick: (ProfileWithMotifs) -> Unit
) {
    Surface(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize()
    ) {
        when (feedState) {
            FeedState.Loading -> {
                CircularProgressIndicator()
            }

            is FeedState.Data -> {
                val profiles = viewModel.profiles.collectAsLazyPagingItems()
                Profiles(
                    items = profiles,
                    modifier = modifier,
                    onClick = onProfileClick
                )
            }

            else -> {}
        }
    }
}

public fun <T : Any> LazyGridScope.items(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((T?) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(value: T?) -> Unit
) {
    items(lazyPagingItems.itemCount) { index ->
        itemContent(lazyPagingItems[index])
    }
}

enum class Recentness {
    TODAY,
    YESTERDAY,
    LAST_WEEK,
    OLDER
}

val Recentness.label: String
    get() = when (this) {
        Recentness.TODAY -> "Today"
        Recentness.YESTERDAY -> "Yesterday"
        Recentness.LAST_WEEK -> "Last week"
        Recentness.OLDER -> "Older"
    }

@Composable
fun Profiles(
    items: LazyPagingItems<FeedItem>,
    modifier: Modifier = Modifier,
    onClick: (ProfileWithMotifs) -> Unit
) {
    LazyVerticalGrid(
        GridCells.Adaptive(minSize = 72.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(16.dp)
    ) {
        for (i in 0 until items.itemCount) {
            when (val item = items[i]) {
                null -> item {
                    FeedProfile(null, onClick = {})
                }

                is FeedItem.Item -> item(item.data.profile.id) {
                    FeedProfile(item.data, onClick = { item.data.let(onClick) })
                }

                is FeedItem.Header -> item(item.recentness.name, span = {
                    GridItemSpan(maxLineSpan)
                }) {
                    Text(
                        text = item.recentness.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun FeedProfile(
    profile: ProfileWithMotifs?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        val borderStroke: @Composable Modifier.() -> Modifier = if (profile?.anyUnlistened != true) {
            {
                border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12F),
                    shape = CircleShape
                )
            }
        } else {
            {
                border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(listOf(PrimaryLight, PrimaryDark)),
                    shape = CircleShape
                )
            }
        }
        Box(
            modifier = Modifier.borderStroke()
                .padding(5.dp)
        ) {
            AsyncImage(
                profile?.profile?.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1F, matchHeightConstraintsFirst = true)
                    .clip(CircleShape)
                    .placeholder(
                        visible = profile == null,
                        highlight = PlaceholderHighlight.shimmer()
                    ),
                contentScale = ContentScale.Crop
            )
        }
    }
}