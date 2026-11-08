package com.example.feature.details

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.domain.model.WatchHistoryItem
import com.example.domain.model.WatchStatus
import com.example.ui.theme.AniBackground
import com.example.ui.theme.AniBorder
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniSurfaceElevated
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsScreen(
    state: AppResult<Anime>,
    inWatchlist: Boolean,
    episodeProgressMap: Map<Int, WatchHistoryItem>,
    personalRating: Float?,
    defaultAudioType: String,
    onBack: () -> Unit,
    onPlayEpisode: (Int, String, String, Int) -> Unit,
    onToggleWatchlist: (Anime) -> Unit,
    onSetWatchStatus: (Anime, WatchStatus) -> Unit,
    onSaveRating: (Float) -> Unit,
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
    var showDownloadInfoSheet by remember { mutableStateOf(false) }
    var selectedChunkIndex by remember { mutableIntStateOf(0) }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                EmptyStateView(
                    title = "Could Not Load Anime",
                    subtitle = state.message,
                    illustrationType = EmptyIllustrationType.NOT_FOUND_404,
                    actionButtonText = "Retry",
                    onActionClick = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        is AppResult.Success -> {
            val anime = state.data
            val totalEps = anime.availableEpisodeCount.coerceAtLeast(1)
            val resumeEpisode = episodeProgressMap.values
                .maxByOrNull { it.lastWatchedAtMs }
                ?.episode
                ?: 1

            val episodeNumbers = (1..totalEps).toList()
            val chunks = episodeNumbers.chunked(25).ifEmpty { listOf(listOf(1)) }
            val currentChunk = chunks.getOrElse(selectedChunkIndex) { chunks.first() }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .testTag("anime_details_screen"),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Hero Cover Artwork matching Screenshot 32
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .background(AniCardBackground)
                    ) {
                        AsyncImage(
                            model = anime.coverUrl?.takeIf { it.isNotBlank() } ?: anime.posterUrl,
                            contentDescription = anime.displayTitle,
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
                                            0.4f to Color.Transparent,
                                            1.0f to AniBackground
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("details_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Title, Score, Badges, Play/Download Buttons, Synopsis
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.displayTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (!anime.nativeTitle.isNullOrBlank() || anime.romajiTitle != anime.displayTitle) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = listOfNotNull(
                                            anime.romajiTitle.takeIf { it != anime.displayTitle },
                                            anime.nativeTitle
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AniTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { showStatusSheet = true },
                                    modifier = Modifier.testTag("details_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save to My List",
                                        tint = if (inWatchlist) AniGreen else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                IconButton(
                                    onClick = { showShareSheet = true },
                                    modifier = Modifier.testTag("details_share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Score + Year + Outlined Badges Row matching Screenshot 32
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showRatingSheet = true }
                                    .padding(vertical = 4.dp)
                                    .testTag("open_rating_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = AniGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = personalRating?.let { String.format(java.util.Locale.US, "%.1f", it * 2f) }
                                        ?: anime.formattedScore,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = AniGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = (anime.releaseYear ?: 2024).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            OutlinedMetaBadge(text = if (anime.isAdult) "18+" else "13+")
                            OutlinedMetaBadge(text = anime.regionDisplay)
                            OutlinedMetaBadge(text = selectedAudioType.uppercase())
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SUB / DUB Audio Selector Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Audio Stream:",
                                style = MaterialTheme.typography.labelLarge,
                                color = AniTextSecondary
                            )
                            SelectablePillChip(
                                text = "SUB (Japanese)",
                                selected = selectedAudioType.equals("sub", ignoreCase = true),
                                onClick = { selectedAudioType = "sub" },
                                testTag = "audio_sub_chip"
                            )
                            SelectablePillChip(
                                text = "DUB (English)",
                                selected = selectedAudioType.equals("dub", ignoreCase = true),
                                onClick = { selectedAudioType = "dub" },
                                testTag = "audio_dub_chip"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Play + Download Buttons Row matching Screenshot 32
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GreenPrimaryButton(
                                text = if (resumeEpisode > 1) "Play Ep $resumeEpisode" else "Play",
                                icon = Icons.Default.PlayArrow,
                                onClick = {
                                    onPlayEpisode(
                                        resumeEpisode,
                                        selectedAudioType,
                                        anime.displayTitle,
                                        totalEps
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                testTag = "details_play_button"
                            )
                            GreenOutlinedButton(
                                text = "Download",
                                icon = Icons.Default.Download,
                                onClick = { showDownloadInfoSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                testTag = "details_download_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Genres & Studio Info
                        Text(
                            text = "Genre: ${anime.genres.joinToString(", ").ifBlank { "Anime" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AniTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (anime.studios.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Studio: ${anime.studios.joinToString(", ")} • ${anime.format ?: "TV"} • ${anime.status ?: "RELEASING"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AniTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Expandable Synopsis with green "View More"
                        val desc = anime.description.ifBlank { "No description available for this anime." }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AniTextSecondary,
                            maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (desc.length > 140) {
                            Text(
                                text = if (synopsisExpanded) "Show Less" else "View More",
                                style = MaterialTheme.typography.labelLarge,
                                color = AniGreen,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                            )
                        }
                    }
                }

                // Episodes Section matching Screenshot 32
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes ($totalEps)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (chunks.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedChunkIndex = (selectedChunkIndex + 1) % chunks.size
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                val firstEp = currentChunk.first()
                                val lastEp = currentChunk.last()
                                Text(
                                    text = "Eps $firstEp-$lastEp",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = AniGreen
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = anime.season?.lowercase()?.replaceFirstChar { it.uppercase() }
                                        ?: "Season 1",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val metaByNum = remember(anime.streamingEpisodes) {
                        anime.streamingEpisodes.associateBy { it.number }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentChunk, key = { "ep_$it" }) { epNum ->
                            val epMeta = metaByNum[epNum]
                            val progressItem = episodeProgressMap[epNum]
                            EpisodeThumbnailCard(
                                episodeNumber = epNum,
                                episodeTitle = epMeta?.title ?: "Episode $epNum",
                                thumbnailUrl = epMeta?.thumbnailUrl ?: anime.coverUrl ?: anime.posterUrl,
                                progressFraction = progressItem?.progressFraction ?: 0f,
                                isCompleted = progressItem?.isCompleted == true,
                                onClick = {
                                    onPlayEpisode(
                                        epNum,
                                        selectedAudioType,
                                        anime.displayTitle,
                                        totalEps
                                    )
                                }
                            )
                        }
                    }
                }

                // More Like This & Details/Cast Tabs matching Screenshot 32
                item {
                    Spacer(modifier = Modifier.height(22.dp))
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
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
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
                                    text = "Related & Cast (${anime.relations.size + anime.characters.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) AniGreen else AniTextSecondary
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (selectedTab == 0) {
                    val recs = anime.recommendations.ifEmpty { anime.relations }
                    val rows = recs.chunked(2)
                    items(rows) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { rec ->
                                AnimePosterCard(
                                    anime = rec,
                                    onClick = { onAnimeClick(rec) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    if (anime.relations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Related Anime & Seasons",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(anime.relations, key = { "rel_${it.anilistId}" }) { rel ->
                                    AnimePosterCard(
                                        anime = rel,
                                        onClick = { onAnimeClick(rel) },
                                        modifier = Modifier.width(132.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (anime.characters.isNotEmpty()) {
                        item {
                            Text(
                                text = "Main Characters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(anime.characters, key = { "char_${it.id}_${it.name}" }) { ch ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(88.dp)
                                    ) {
                                        AsyncImage(
                                            model = ch.imageUrl,
                                            contentDescription = ch.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(AniCardBackground)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = ch.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ch.role,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AniTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showRatingSheet) {
                GiveRatingBottomSheet(
                    animeScore = anime.score ?: 9.5,
                    popularityCount = anime.popularity,
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
                    anime = anime,
                    onDismiss = { showShareSheet = false },
                    onShareExternal = {
                        showShareSheet = false
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Watch ${anime.displayTitle} on AniScroll! https://anilist.co/anime/${anime.anilistId}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${anime.displayTitle}"))
                    }
                )
            }

            if (showStatusSheet) {
                WatchlistStatusBottomSheet(
                    anime = anime,
                    inWatchlist = inWatchlist,
                    onDismiss = { showStatusSheet = false },
                    onSelectStatus = { status ->
                        onSetWatchStatus(anime, status)
                        showStatusSheet = false
                    },
                    onToggleQuick = {
                        onToggleWatchlist(anime)
                        showStatusSheet = false
                    }
                )
            }

            if (showDownloadInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showDownloadInfoSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = AniSecondaryBackground
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Offline Download Unavailable",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The AniScroll streaming endpoint (api.aniscroll.in) provides authorized live HLS streams only and does not currently provide offline downloadable files. You can stream all episodes immediately in SUB or DUB using the Play button.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AniTextSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        GreenPrimaryButton(
                            text = "Stream Episode Now",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                showDownloadInfoSheet = false
                                onPlayEpisode(resumeEpisode, selectedAudioType, anime.displayTitle, totalEps)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OutlinedMetaBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, AniGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = AniGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EpisodeThumbnailCard(
    episodeNumber: Int,
    episodeTitle: String,
    thumbnailUrl: String,
    progressFraction: Float,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(156.dp)
            .aspectRatio(1.42f)
            .clip(RoundedCornerShape(12.dp))
            .background(AniCardBackground)
            .clickable(onClick = onClick)
            .testTag("episode_card_$episodeNumber")
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = episodeTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.78f))
                    )
                )
        )
        Surface(
            color = Color.White.copy(alpha = 0.90f),
            shape = CircleShape,
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Episode $episodeNumber",
                tint = Color.Black,
                modifier = Modifier.padding(5.dp)
            )
        }
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Watched",
                tint = AniGreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
            )
        }
        Text(
            text = "Episode $episodeNumber",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
        if (progressFraction > 0f) {
            LinearProgressIndicator(
                progress = { progressFraction },
                color = AniGreen,
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiveRatingBottomSheet(
    animeScore: Double,
    popularityCount: Int,
    initialStars: Float,
    onDismiss: () -> Unit,
    onSubmit: (Float) -> Unit
) {
    var selectedStars by remember { mutableFloatStateOf(initialStars) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AniSecondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Give Rating",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AniTextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = AniBorder)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.4f)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", animeScore),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        Text(
                            text = " /10",
                            style = MaterialTheme.typography.titleMedium,
                            color = AniTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AniGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "(${popularityCount.coerceAtLeast(1250)} users)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AniTextSecondary
                    )
                }

                Column(
                    modifier = Modifier.weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bars = listOf(5 to 0.88f, 4 to 0.72f, 3 to 0.18f, 2 to 0.06f, 1 to 0.03f)
                    bars.forEach { (star, ratio) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = star.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = AniTextPrimary
                            )
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = AniGreen,
                                trackColor = AniCardBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = AniBorder)
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    val filled = i <= selectedStars.toInt()
                    Icon(
                        imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Rate $i stars",
                        tint = AniGreen,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { selectedStars = i.toFloat() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniSurfaceElevated,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
                GreenPrimaryButton(
                    text = "Submit",
                    onClick = { onSubmit(selectedStars) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    anime: Anime,
    onDismiss: () -> Unit,
    onShareExternal: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AniSecondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Share to",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AniTextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = AniBorder)
            Spacer(modifier = Modifier.height(18.dp))

            val targets = listOf(
                "WhatsApp" to Color(0xFF25D366),
                "Twitter" to Color(0xFF1DA1F2),
                "Facebook" to Color(0xFF1877F2),
                "Instagram" to Color(0xFFE1306C),
                "Telegram" to Color(0xFF0088CC),
                "Discord" to Color(0xFF5865F2),
                "Messages" to AniGreen,
                "More" to AniSurfaceElevated
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                targets.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { (name, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onShareExternal)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                ) {
                                    Text(
                                        text = name.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AniTextSecondary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistStatusBottomSheet(
    anime: Anime,
    inWatchlist: Boolean,
    onDismiss: () -> Unit,
    onSelectStatus: (WatchStatus) -> Unit,
    onToggleQuick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AniSecondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Save to My List",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AniTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = anime.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AniTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            WatchStatus.entries.forEach { status ->
                Surface(
                    color = AniCardBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectStatus(status) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = AniTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AniGreen
                        )
                    }
                }
            }

            if (inWatchlist) {
                Spacer(modifier = Modifier.height(12.dp))
                GreenOutlinedButton(
                    text = "Remove from My List",
                    onClick = onToggleQuick,
                    borderColor = Color(0xFFFF4545),
                    contentColor = Color(0xFFFF4545),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
