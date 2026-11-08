package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.TimeUtils
import com.example.core.designsystem.AniScrollLogo
import com.example.core.designsystem.AnimePosterCard
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SectionHeaderRow
import com.example.core.designsystem.ShimmerBox
import com.example.domain.model.Anime
import com.example.domain.model.WatchHistoryItem
import com.example.feature.common.HomeUiState
import com.example.ui.theme.AniBackground
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    watchlistIds: Set<Int>,
    continueWatching: List<WatchHistoryItem>,
    recentlyWatched: List<WatchHistoryItem>,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSeeAllTopHits: () -> Unit,
    onSeeAllNewReleases: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onPlayAnime: (Anime) -> Unit,
    onResumeHistory: (WatchHistoryItem) -> Unit,
    onToggleWatchlist: (Anime) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Featured Hero Banner (matches Screenshot 21_Dark_home)
        item {
            FeaturedHeroBanner(
                anime = uiState.dashboard.featuredAnime,
                isLoading = uiState.isLoading,
                inWatchlist = uiState.dashboard.featuredAnime?.anilistId in watchlistIds,
                onSearchClick = onSearchClick,
                onNotificationsClick = onNotificationsClick,
                onPlayClick = { uiState.dashboard.featuredAnime?.let(onPlayAnime) },
                onDetailsClick = { uiState.dashboard.featuredAnime?.let(onAnimeClick) },
                onToggleWatchlist = { uiState.dashboard.featuredAnime?.let(onToggleWatchlist) }
            )
        }

        if (uiState.errorMessage != null && uiState.dashboard.topHits.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AniTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GreenPrimaryButton(
                        text = "Retry",
                        icon = Icons.Default.Refresh,
                        onClick = onRetry,
                        testTag = "home_retry_button"
                    )
                }
            }
        }

        // 2. Continue Watching
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.continue_watching))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continueWatching, key = { "cw_${it.anilistId}_${it.episode}" }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = { onResumeHistory(item) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 3. Top Hits Anime
        item {
            SectionHeaderRow(
                title = stringResource(R.string.top_hits_anime),
                onSeeAllClick = onSeeAllTopHits
            )
            if (uiState.isLoading && uiState.dashboard.topHits.isEmpty()) {
                ShimmerCarouselRow()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        uiState.dashboard.topHits,
                        key = { _, anime -> "top_${anime.anilistId}" }
                    ) { index, anime ->
                        AnimePosterCard(
                            anime = anime,
                            rankNumber = index + 1,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. New Episode Releases
        item {
            SectionHeaderRow(
                title = stringResource(R.string.new_episode_releases),
                onSeeAllClick = onSeeAllNewReleases
            )
            if (uiState.isLoading && uiState.dashboard.newEpisodeReleases.isEmpty()) {
                ShimmerCarouselRow()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        uiState.dashboard.newEpisodeReleases,
                        key = { "new_${it.anilistId}" }
                    ) { anime ->
                        val epNum = anime.nextAiringEpisode?.let { (it - 1).coerceAtLeast(1) }
                            ?: anime.totalEpisodes
                            ?: 1
                        AnimePosterCard(
                            anime = anime,
                            episodeLabel = "Episode $epNum",
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. Trending Anime
        if (uiState.dashboard.trendingAnime.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.trending_anime))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.dashboard.trendingAnime, key = { "trend_${it.anilistId}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 6. Popular This Season
        if (uiState.dashboard.seasonalAnime.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.popular_this_season))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.dashboard.seasonalAnime, key = { "season_${it.anilistId}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 7. Recommended Anime
        if (uiState.dashboard.recommendedAnime.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.recommended_anime))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.dashboard.recommendedAnime, key = { "rec_${it.anilistId}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 8. Popular Movies
        if (uiState.dashboard.popularMovies.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.popular_movies))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.dashboard.popularMovies, key = { "movie_${it.anilistId}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            episodeLabel = "Movie",
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 9. Upcoming Anime
        if (uiState.dashboard.upcomingAnime.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.upcoming_anime))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.dashboard.upcomingAnime, key = { "up_${it.anilistId}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            episodeLabel = "Upcoming",
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 10. Recently Watched
        if (recentlyWatched.isNotEmpty()) {
            item {
                SectionHeaderRow(title = stringResource(R.string.recently_watched))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyWatched.take(10), key = { "recent_${it.anilistId}_${it.episode}" }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = { onResumeHistory(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeroBanner(
    anime: Anime?,
    isLoading: Boolean,
    inWatchlist: Boolean,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .background(AniCardBackground)
            .clickable(enabled = anime != null, onClick = onDetailsClick)
    ) {
        if (anime != null) {
            AsyncImage(
                model = anime.coverUrl?.takeIf { it.isNotBlank() } ?: anime.posterUrl,
                contentDescription = anime.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
        }

        // Top & Bottom cinematic gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to AniBackground.copy(alpha = 0.65f),
                            0.30f to Color.Transparent,
                            0.62f to AniBackground.copy(alpha = 0.55f),
                            1.0f to AniBackground
                        )
                    )
                )
        )

        // Top App Bar inside Hero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AniScrollLogo(size = 34.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("home_notifications_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Bottom Hero Metadata + Play & My List Buttons
        if (anime != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = anime.genres.ifEmpty { listOf("Action", "Adventure", "Anime") }.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AniTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GreenPrimaryButton(
                        text = stringResource(R.string.play),
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayClick,
                        testTag = "hero_play_button"
                    )
                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("hero_mylist_button")
                    ) {
                        Icon(
                            imageVector = if (inWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.my_list),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: WatchHistoryItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AniCardBackground)
            .clickable(onClick = onClick)
            .testTag("continue_watching_${item.anilistId}_${item.episode}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
        ) {
            AsyncImage(
                model = item.coverUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl,
                contentDescription = item.animeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
            Surface(
                color = Color.White.copy(alpha = 0.90f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = AniBackground,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Text(
                text = "Ep ${item.episode} • ${TimeUtils.formatDurationMs(item.positionMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
        LinearProgressIndicator(
            progress = { item.progressFraction },
            color = AniGreen,
            trackColor = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )
        Text(
            text = item.animeTitle,
            style = MaterialTheme.typography.labelMedium,
            color = AniTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ShimmerCarouselRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            ShimmerBox(
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(0.71f)
            )
        }
    }
}
