package de.julianostarek.motif.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.pager.rememberPagerState
import de.julianostarek.motif.feed.FeedState
import de.julianostarek.motif.ui.Keyline1
import de.julianostarek.motif.feed.domain.FeedMotifGroup

@Composable
fun Feed(
    viewModel: AndroidFeedViewModel = viewModel()
) {
    val viewState by viewModel.state.collectAsState()
    Surface(Modifier.fillMaxSize()) {
        when (val cast = viewState) {
            FeedState.Loading -> {
                CircularProgressIndicator()
            }

            is FeedState.Data -> FeedContent(cast.motifGroups)
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
    motifs: List<FeedMotifGroup>
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

            if (motifs.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                val pagerState = rememberPagerState()

                MotifGroups(
                    motifGroups = motifs,
                    modifier = Modifier
                        .padding(start = Keyline1, top = 16.dp, end = Keyline1)
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MotifGroups(motifGroups: List<FeedMotifGroup>, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier) {
        itemsIndexed(motifGroups) { index, item ->
            AsyncImage(
                item.creator.photoUrl,
                contentDescription = null,
                modifier = Modifier.clip(CircleShape)
                    .padding(16.dp)
            )
        }
    }
}