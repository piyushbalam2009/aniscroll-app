package com.example.feature.movies

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.common.AppResult
import com.example.core.designsystem.AnimePosterCard
import com.example.core.designsystem.EmptyIllustrationType
import com.example.core.designsystem.EmptyStateView
import com.example.core.designsystem.GreenOutlinedButton
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SelectablePillChip
import com.example.core.designsystem.SpinningDotsLoader
import com.example.domain.model.Anime
import com.example.domain.model.HlsPlaylistInspection
import com.example.domain.model.HlsQualityVariant
import com.example.domain.model.OfflineDownloadItem
import com.example.domain.model.WatchStatus
import com.example.feature.details.GiveRatingBottomSheet
import com.example.feature.details.HlsDownloadEpisodeSheet
import com.example.feature.details.OutlinedMetaBadge
import com.example.feature.details.ShareBottomSheet
import com.example.feature.details.WatchlistStatusBottomSheet
import com.example.ui.theme.AniBackground
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniTextSecondary

@Composable
fun MovieDetailsScreen(
    state: AppResult<Anime>,
    inWatchlist: Boolean,
    movieDownloadsMap: Map<Int, OfflineDownloadItem> = emptyMap(),
    personalRating: Float?,
    defaultAudioType: String,
    onBack: () -> Unit,
    onPlayMovie: (String, String) -> Unit,
    onToggleWatchlist: (Anime) -> Unit,
    onSetWatchStatus: (Anime, WatchStatus) -> Unit,
    onSaveRating: (Float) -> Unit,
    onInspectEpisodeDownload: suspend (Anime, Int, String) -> AppResult<HlsPlaylistInspection> = { _, _, _ ->
        AppResult.Error("Offline download inspector not configured.")
    },
    onStartEpisodeDownload: suspend (HlsPlaylistInspection, HlsQualityVariant) -> AppResult<OfflineDownloadItem> = { _, _ ->
        AppResult.Error("Offline download manager not configured.")
    },
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelOrDeleteDownload: (String) -> Unit = {},
    onOpenDownloadsScreen: () -> Unit = {},
    onAnimeClick: (Anime) -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    var selectedAudioType by remember(defaultAudioType) { mutableStateOf(defaultAudioType) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }

    when (state) {
        is AppResult.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                SpinningDotsLoader()
            }
        }

        is AppResult.Error -> {
            EmptyStateView(
                title = "Could Not Load Movie",
                subtitle = state.message,
                illustrationType = EmptyIllustrationType.NOT_FOUND_404,
                actionButtonText = "Retry",
                onActionClick = onRetry,
                modifier = Modifier.fillMaxSize()
            )
        }

        is AppResult.Success -> {
            val movie = state.data
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .testTag("movie_details_screen"),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Full-bleed Movie Poster/Cover matching Screenshot 42_Dark_anime movie details
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(AniCardBackground)
                    ) {
                        AsyncImage(
                            model = movie.posterUrl.ifBlank { movie.coverUrl },
                            contentDescription = movie.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to AniBackground.copy(alpha = 0.55f),
                                            0.45f to Color.Transparent,
                                            1.0f to AniBackground
                                        )
                                    )
                                )
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("movie_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = movie.displayTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = { showStatusSheet = true }) {
                                    Icon(
                                        imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save Movie",
                                        tint = if (inWatchlist) AniGreen else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                IconButton(onClick = { showShareSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Movie",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showRatingSheet = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Score",
                                    tint = AniGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = movie.formattedScore,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = (movie.releaseYear ?: 2024).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            OutlinedMetaBadge("MOVIE")
                            OutlinedMetaBadge(movie.regionDisplay)
                            OutlinedMetaBadge(selectedAudioType.uppercase())
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SelectablePillChip(
                                text = "SUB",
                                selected = selectedAudioType == "sub",
                                onClick = { selectedAudioType = "sub" }
                            )
                            SelectablePillChip(
                                text = "DUB",
                                selected = selectedAudioType == "dub",
                                onClick = { selectedAudioType = "dub" }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GreenPrimaryButton(
                                text = "Play Movie",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onPlayMovie(selectedAudioType, movie.displayTitle) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                testTag = "play_movie_button"
                            )
                            GreenOutlinedButton(
                                text = "Download",
                                icon = Icons.Default.Download,
                                onClick = { showDownloadSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                testTag = "download_movie_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Genre: ${movie.genres.joinToString(", ").ifBlank { "Feature Film" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AniTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val desc = movie.description.ifBlank { "No synopsis available." }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AniTextSecondary,
                            maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (desc.length > 160) {
                            Text(
                                text = if (synopsisExpanded) "Show Less" else "View More",
                                style = MaterialTheme.typography.labelLarge,
                                color = AniGreen,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = AniGreen,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    height = 3.dp,
                                    color = AniGreen
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        text = "More Like This",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 0) AniGreen else AniTextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(
                                        text = "Related Anime",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 1) AniGreen else AniTextSecondary
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                val listToShow = if (selectedTab == 0) {
                    movie.recommendations.ifEmpty { movie.relations }
                } else {
                    movie.relations.ifEmpty { movie.recommendations }
                }

                items(listToShow.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { item ->
                            AnimePosterCard(
                                anime = item,
                                onClick = { onAnimeClick(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (showRatingSheet) {
                GiveRatingBottomSheet(
                    animeScore = movie.score ?: 9.5,
                    popularityCount = movie.popularity,
                    initialStars = personalRating ?: 5f,
                    onDismiss = { showRatingSheet = false },
                    onSubmit = { stars ->
                        onSaveRating(stars)
                        showRatingSheet = false
                    }
                )
            }

            if (showShareSheet) {
                ShareBottomSheet(
                    anime = movie,
                    onDismiss = { showShareSheet = false },
                    onShareExternal = {
                        showShareSheet = false
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Watch ${movie.displayTitle} on AniScroll! https://anilist.co/anime/${movie.anilistId}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${movie.displayTitle}"))
                    }
                )
            }

            if (showStatusSheet) {
                WatchlistStatusBottomSheet(
                    anime = movie,
                    inWatchlist = inWatchlist,
                    onDismiss = { showStatusSheet = false },
                    onSelectStatus = {
                        onSetWatchStatus(movie, it)
                        showStatusSheet = false
                    },
                    onToggleQuick = {
                        onToggleWatchlist(movie)
                        showStatusSheet = false
                    }
                )
            }

            if (showDownloadSheet) {
                HlsDownloadEpisodeSheet(
                    anime = movie,
                    initialEpisode = 1,
                    initialAudioType = selectedAudioType,
                    existingDownloads = movieDownloadsMap,
                    onInspectEpisodeDownload = onInspectEpisodeDownload,
                    onStartEpisodeDownload = onStartEpisodeDownload,
                    onPauseDownload = onPauseDownload,
                    onResumeDownload = onResumeDownload,
                    onCancelOrDeleteDownload = onCancelOrDeleteDownload,
                    onPlayOfflineEpisode = { _, audioType ->
                        onPlayMovie(audioType, movie.displayTitle)
                    },
                    onStreamOnlineEpisode = { _, audioType ->
                        onPlayMovie(audioType, movie.displayTitle)
                    },
                    onOpenDownloadsScreen = onOpenDownloadsScreen,
                    onDismiss = { showDownloadSheet = false }
                )
            }
        }
    }
}
