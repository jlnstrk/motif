package de.julianostarek.motif.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.pager.rememberPagerState
import de.julianostarek.motif.feed.FeedState
import de.julianostarek.motif.feed.SquareGrid
import de.julianostarek.motif.ui.Keyline1
import de.julianostarek.motif.domain.ProfileWithMotifs
import de.julianostarek.motif.ui.player.AndroidPlayerViewModel

@Composable
fun Feed(
    playerViewModel: AndroidPlayerViewModel,
    feedViewModel: AndroidFeedViewModel = viewModel()
) {
    val viewState by feedViewModel.state.collectAsState()
    Surface(Modifier.fillMaxSize()) {
        when (val cast = viewState) {
            FeedState.Loading -> {
                CircularProgressIndicator()
            }

            is FeedState.Data -> FeedContent(cast.profilesGrid,
                onClicked = { profileWithMotifs ->
                    playerViewModel.shared.play(profileWithMotifs.motifs.first())
                })

            else -> {}
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

@OptIn(ExperimentalPagerApi::class) // HorizontalPager is experimental
@Composable
fun FeedContent(
    profiles: SquareGrid<ProfileWithMotifs?>,
    onClicked: (ProfileWithMotifs) -> Unit
) {
    Column(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val appBarColor = MaterialTheme.colors.surface.copy(alpha = 0.87f)

            // Draw a scrim over the status bar which matches the app bar
            Spacer(
                Modifier
                    .background(appBarColor)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
            )

            FeedAppBar(
                backgroundColor = appBarColor,
                modifier = Modifier.fillMaxWidth()
            )

            if (profiles.grid.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                val pagerState = rememberPagerState()

                MotifGroups(
                    profiles = profiles,
                    modifier = Modifier
                        .padding(start = Keyline1, top = 16.dp, end = Keyline1)
                        .fillMaxWidth()
                        .height(200.dp),
                    onClicked
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MotifGroups(
    profiles: SquareGrid<ProfileWithMotifs?>,
    modifier: Modifier = Modifier,
    onClicked: (ProfileWithMotifs) -> Unit
) {
    val columns = GridCells.Fixed(profiles.size)
    val horizontalScrollState = rememberScrollState()
    Box(Modifier.horizontalScroll(horizontalScrollState)) {
        Box(
            Modifier.wrapContentWidth(align = Alignment.CenterHorizontally, unbounded = true)

        ) {
            Box(
                Modifier
                    .width((72 + 8).dp * profiles.size - 8.dp)
            ) {
                LazyVerticalGrid(
                    columns,
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(profiles.grid, { _, item -> item?.profile?.id ?: Any() }) { index, item ->
                        if (item != null) {
                            var imageModifier: Modifier = Modifier
                            if (index / profiles.size % 2 == 0) {
                                imageModifier = imageModifier.offset(x = 32.dp)
                            }
                            AsyncImage(
                                item.profile.photoUrl,
                                contentDescription = null,
                                modifier = imageModifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable { onClicked(item) },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Spacer(Modifier.size(72.dp))
                        }
                    }
                }
            }
        }
    }
}