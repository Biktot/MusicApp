/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.podcast.PodcastLibraryItemUiModel
import moe.rukamori.archivetune.podcast.PodcastLibraryScreenState
import moe.rukamori.archivetune.ui.component.MediaDetailStatePanel
import moe.rukamori.archivetune.ui.utils.YtimgResizePolicy
import moe.rukamori.archivetune.ui.utils.resize
import moe.rukamori.archivetune.viewmodels.PodcastLibraryViewModel

@Composable
fun LibraryPodcastsScreen(
    navController: NavController,
    viewModel: PodcastLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val onRefresh = remember(viewModel) { { viewModel.sync() } }
    val onRefreshErrorShown = remember(viewModel) { { viewModel.onRefreshErrorShown() } }
    val onPodcastClick =
        remember(navController) {
            { browseId: String -> navController.navigate("podcast/${Uri.encode(browseId)}") }
        }

    LibraryRefreshContainer(
        state = refreshState,
        onRefresh = onRefresh,
        onErrorShown = onRefreshErrorShown,
        modifier = Modifier.fillMaxSize(),
    ) {
        LibraryPodcastsContent(
            state = state,
            onRetry = onRefresh,
            onPodcastClick = onPodcastClick,
        )
    }
}

@Composable
private fun BoxScope.LibraryPodcastsContent(
    state: PodcastLibraryScreenState,
    onRetry: () -> Unit,
    onPodcastClick: (String) -> Unit,
) {
    val bottomPadding =
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

    when (state) {
        PodcastLibraryScreenState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        PodcastLibraryScreenState.Empty -> {
            MediaDetailStatePanel(
                title = stringResource(R.string.podcast),
                description = stringResource(R.string.browse_empty_description),
                iconRes = R.drawable.mic,
                actionLabel = stringResource(R.string.retry),
                onAction = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        is PodcastLibraryScreenState.Error -> {
            MediaDetailStatePanel(
                title = stringResource(R.string.podcast),
                description = stringResource(state.messageResId),
                iconRes = R.drawable.error,
                actionLabel = stringResource(R.string.retry),
                onAction = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        is PodcastLibraryScreenState.Success -> {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        top = LibraryHeaderContentPadding,
                        end = 16.dp,
                        bottom = bottomPadding + 16.dp,
                    ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.uiState.podcasts,
                    key = PodcastLibraryItemUiModel::browseId,
                    contentType = { "library_podcast" },
                ) { podcast ->
                    PodcastLibraryRow(
                        podcast = podcast,
                        onClick = remember(podcast.browseId, onPodcastClick) { { onPodcastClick(podcast.browseId) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastLibraryRow(
    podcast: PodcastLibraryItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val author = podcast.author
    val sourceLabel =
        stringResource(
            if (podcast.isSavedRemotely) {
                R.string.youtube_synced
            } else {
                R.string.personal_label
            },
        )
    val imageModel =
        remember(podcast.thumbnailUrl) {
            podcast.thumbnailUrl?.resize(
                width = PodcastLibraryArtworkDecodeSize,
                height = PodcastLibraryArtworkDecodeSize,
                ytimgResizePolicy = YtimgResizePolicy.PreserveOriginal,
            )
        }
    ListItem(
        headlineContent = {
            Text(
                text = podcast.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                author?.let {
                    Text(
                        text = author,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = sourceLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(PodcastLibraryArtworkSize)
                        .clip(RoundedCornerShape(16.dp)),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            modifier
                .heightIn(min = 88.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onClick),
    )
}

private const val PodcastLibraryArtworkDecodeSize = 192
private val PodcastLibraryArtworkSize = 72.dp
