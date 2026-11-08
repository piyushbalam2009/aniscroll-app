package com.example.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.AppResult
import com.example.core.designsystem.AnimePosterCard
import com.example.core.designsystem.EmptyIllustrationType
import com.example.core.designsystem.EmptyStateView
import com.example.core.designsystem.SpinningDotsLoader
import com.example.core.designsystem.TopHitAnimeRowCard
import com.example.domain.model.Anime

@Composable
fun TopHitsScreen(
    state: AppResult<List<Anime>>,
    watchlistIds: Set<Int>,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onToggleWatchlist: (Anime) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("top_hits_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("top_hits_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.top_hits_anime),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onSearchClick, modifier = Modifier.testTag("top_hits_search")) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        when (state) {
            is AppResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SpinningDotsLoader()
                }
            }
            is AppResult.Error -> {
                EmptyStateView(
                    title = "Could Not Load Top Hits",
                    subtitle = state.message,
                    illustrationType = EmptyIllustrationType.NOT_FOUND_404,
                    actionButtonText = "Retry",
                    onActionClick = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AppResult.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        state.data,
                        key = { _, anime -> anime.anilistId }
                    ) { index, anime ->
                        TopHitAnimeRowCard(
                            anime = anime,
                            rank = index + 1,
                            inWatchlist = anime.anilistId in watchlistIds,
                            onCardClick = { onAnimeClick(anime) },
                            onToggleWatchlist = { onToggleWatchlist(anime) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewEpisodeReleasesScreen(
    state: AppResult<List<Anime>>,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("new_releases_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("new_releases_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.new_episode_releases),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onSearchClick, modifier = Modifier.testTag("new_releases_search")) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        when (state) {
            is AppResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SpinningDotsLoader()
                }
            }
            is AppResult.Error -> {
                EmptyStateView(
                    title = "Could Not Load Releases",
                    subtitle = state.message,
                    illustrationType = EmptyIllustrationType.NOT_FOUND_404,
                    actionButtonText = "Retry",
                    onActionClick = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AppResult.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.data, key = { it.anilistId }) { anime ->
                        val epNum = anime.nextAiringEpisode?.let { (it - 1).coerceAtLeast(1) }
                            ?: anime.totalEpisodes
                            ?: 1
                        AnimePosterCard(
                            anime = anime,
                            episodeLabel = "Episode $epNum",
                            onClick = { onAnimeClick(anime) }
                        )
                    }
                }
            }
        }
    }
}
