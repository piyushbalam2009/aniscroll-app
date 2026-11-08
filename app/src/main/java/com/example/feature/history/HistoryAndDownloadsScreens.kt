package com.example.feature.history

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.TimeUtils
import com.example.core.designsystem.AniScrollLogo
import com.example.core.designsystem.EmptyIllustrationType
import com.example.core.designsystem.EmptyStateView
import com.example.core.designsystem.GreenPrimaryButton
import com.example.domain.model.WatchHistoryItem
import com.example.ui.theme.AniBorder
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniError
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniGreenSurface
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniSurfaceElevated
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<WatchHistoryItem>,
    onResumeItem: (WatchHistoryItem) -> Unit,
    onRemoveItem: (WatchHistoryItem) -> Unit,
    onClearAll: () -> Unit
) {
    var itemToDelete by remember { mutableStateOf<WatchHistoryItem?>(null) }
    var showClearAllSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("history_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AniScrollLogo(size = 30.dp)
                Text(
                    text = "Watch History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (history.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelLarge,
                    color = AniError,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showClearAllSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("clear_all_history_button")
                )
            }
        }

        if (history.isEmpty()) {
            EmptyStateView(
                title = stringResource(R.string.empty_history_title),
                subtitle = stringResource(R.string.empty_history_subtitle),
                illustrationType = EmptyIllustrationType.EMPTY_HISTORY,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { "${it.anilistId}_${it.episode}" }) { item ->
                    HistoryEpisodeRowCard(
                        item = item,
                        onResume = { onResumeItem(item) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Delete item confirmation modal matching Screenshot 48_Dark_delete download modal
    itemToDelete?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { itemToDelete = null },
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
                    text = "Delete",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniError
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AniBorder)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Are you sure you want to remove this episode from your watch history?",
                    style = MaterialTheme.typography.titleMedium,
                    color = AniTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(1.45f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AniCardBackground)
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl,
                            contentDescription = item.animeTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.animeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Episode ${item.episode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AniTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { itemToDelete = null },
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
                        text = "Yes, Delete",
                        onClick = {
                            onRemoveItem(item)
                            itemToDelete = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearAllSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClearAllSheet = false },
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
                    text = "Clear Watch History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniError
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AniBorder)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Are you sure you want to clear all watch history and saved episode progress?",
                    style = MaterialTheme.typography.titleMedium,
                    color = AniTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showClearAllSheet = false },
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
                        text = "Yes, Clear",
                        onClick = {
                            onClearAll()
                            showClearAllSheet = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HistoryEpisodeRowCard(
    item: WatchHistoryItem,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onResume)
            .testTag("history_item_${item.anilistId}_${item.episode}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(138.dp)
                .aspectRatio(1.42f)
                .clip(RoundedCornerShape(12.dp))
                .background(AniCardBackground)
        ) {
            AsyncImage(
                model = item.coverUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl,
                contentDescription = item.animeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.White.copy(alpha = 0.88f),
                shape = CircleShape,
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = Color.Black,
                    modifier = Modifier.padding(5.dp)
                )
            }
            LinearProgressIndicator(
                progress = { item.progressFraction },
                color = AniGreen,
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = item.animeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Episode ${item.episode} • ${TimeUtils.formatRelativeDate(item.lastWatchedAtMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = AniTextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AniGreenSurface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${TimeUtils.formatDurationMs(item.positionMs)} / ${TimeUtils.formatDurationMs(item.durationMs)} (${item.audioType.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = AniGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete History Item",
                        tint = AniGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("downloads_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("downloads_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            AniScrollLogo(size = 28.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.nav_downloads),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        EmptyStateView(
            title = stringResource(R.string.empty_download_title),
            subtitle = stringResource(R.string.empty_download_subtitle),
            illustrationType = EmptyIllustrationType.EMPTY_DOWNLOAD,
            modifier = Modifier.fillMaxSize()
        )
    }
}
