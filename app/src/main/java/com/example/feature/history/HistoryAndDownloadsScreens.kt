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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.core.network.NetworkConnectionState
import com.example.domain.model.OfflineDownloadItem
import com.example.domain.model.OfflineDownloadState
import com.example.domain.model.WatchHistoryItem
import com.example.ui.theme.AniBackground
import com.example.ui.theme.AniBorder
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniError
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniGreenSurface
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniSurfaceElevated
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary
import com.example.ui.theme.AniWarning
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<OfflineDownloadItem> = emptyList(),
    connectionState: NetworkConnectionState = NetworkConnectionState.ONLINE,
    isOnline: Boolean = true,
    storageUsedBytes: Long = 0L,
    storageFreeBytes: Long = 0L,
    wifiOnlyDownloads: Boolean = false,
    onToggleWifiOnly: (Boolean) -> Unit = {},
    onRetryConnection: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onPlayOffline: (OfflineDownloadItem) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelOrDeleteDownload: (String) -> Unit = {},
    onClearAllDownloads: () -> Unit = {},
    onBack: () -> Unit
) {
    val completedDownloads = remember(downloads) {
        downloads.filter { it.isPlayableOffline }
    }
    val activeOrIncompleteDownloads = remember(downloads) {
        downloads.filter { !it.isPlayableOffline }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var downloadToDelete by remember { mutableStateOf<OfflineDownloadItem?>(null) }
    var showClearAllDownloadsSheet by remember { mutableStateOf(false) }

    val totalDownloadedBytes = remember(downloads, storageUsedBytes) {
        val sumFromItems = downloads.sumOf { it.bytesDownloaded.coerceAtLeast(0L) }
        maxOf(storageUsedBytes, sumFromItems)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AniBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("downloads_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("downloads_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AniTextPrimary
                    )
                }
                AniScrollLogo(size = 28.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.nav_downloads),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
            }
            if (downloads.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelLarge,
                    color = AniError,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showClearAllDownloadsSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("clear_all_downloads_button")
                )
            }
        }

        // Connectivity Status Banners (Offline / Reconnecting / Connected Again)
        when {
            !isOnline || connectionState == NetworkConnectionState.OFFLINE -> {
                Surface(
                    color = AniCardBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("offline_mode_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline Mode",
                                tint = AniWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "You're Offline",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AniTextPrimary
                                )
                                Text(
                                    text = "Completed downloads play directly from local storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AniTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = AniGreenSurface,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onRetryConnection)
                                .testTag("banner_retry_connection_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = AniGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AniGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            connectionState == NetworkConnectionState.CONNECTED_AGAIN -> {
                Surface(
                    color = AniGreenSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("internet_connected_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Internet Connected",
                                tint = AniGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Internet Connected",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AniGreen
                                )
                                Text(
                                    text = "Online streaming and anime discovery are available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AniTextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onBackToHome,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("back_to_home_button")
                        ) {
                            Text(
                                text = "Back to Home",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Storage Usage & Wi-Fi Only Card
        StorageAndSettingsSummaryCard(
            usedBytes = totalDownloadedBytes,
            freeBytes = storageFreeBytes,
            completedCount = completedDownloads.size,
            wifiOnlyDownloads = wifiOnlyDownloads,
            onToggleWifiOnly = onToggleWifiOnly
        )

        // Two Tabs: Downloaded | Downloading
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
            divider = { HorizontalDivider(color = AniBorder) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("downloads_tab_row")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("tab_downloaded"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 0) AniGreen else AniTextSecondary
                        )
                        Text(
                            text = "Downloaded (${completedDownloads.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) AniGreen else AniTextSecondary
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("tab_downloading"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Downloading,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 1) AniGreen else AniTextSecondary
                        )
                        Text(
                            text = "Downloading (${activeOrIncompleteDownloads.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) AniGreen else AniTextSecondary
                        )
                    }
                }
            )
        }

        // Main Content Area
        if (!isOnline && downloads.isEmpty()) {
            OfflineNoDownloadsEmptyState(
                onRetryConnection = onRetryConnection,
                modifier = Modifier.fillMaxSize()
            )
        } else if (selectedTab == 0) {
            if (completedDownloads.isEmpty()) {
                if (!isOnline) {
                    OfflineNoDownloadsEmptyState(
                        onRetryConnection = onRetryConnection,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyStateView(
                        title = "No Completed Downloads",
                        subtitle = if (activeOrIncompleteDownloads.isNotEmpty()) {
                            "You have ${activeOrIncompleteDownloads.size} download(s) in progress in the Downloading tab."
                        } else {
                            stringResource(R.string.empty_download_subtitle)
                        },
                        illustrationType = EmptyIllustrationType.EMPTY_DOWNLOAD,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("downloaded_list")
                ) {
                    items(completedDownloads, key = { it.downloadId }) { item ->
                        OfflineDownloadRowCard(
                            item = item,
                            onPlayOffline = { onPlayOffline(item) },
                            onPause = { onPauseDownload(item.downloadId) },
                            onResume = { onResumeDownload(item.downloadId) },
                            onDeleteOrCancel = { downloadToDelete = item }
                        )
                    }
                }
            }
        } else {
            if (activeOrIncompleteDownloads.isEmpty()) {
                EmptyStateView(
                    title = "No Active Downloads",
                    subtitle = if (completedDownloads.isNotEmpty()) {
                        "All ${completedDownloads.size} downloaded episode(s) are ready for offline playback in the Downloaded tab."
                    } else {
                        "Queued, active, paused, and failed episode downloads will appear here."
                    },
                    illustrationType = EmptyIllustrationType.EMPTY_DOWNLOAD,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("downloading_list")
                ) {
                    items(activeOrIncompleteDownloads, key = { it.downloadId }) { item ->
                        OfflineDownloadRowCard(
                            item = item,
                            onPlayOffline = { /* Incomplete downloads cannot be played offline */ },
                            onPause = { onPauseDownload(item.downloadId) },
                            onResume = { onResumeDownload(item.downloadId) },
                            onDeleteOrCancel = { downloadToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Delete / Cancel Download Confirmation Modal
    downloadToDelete?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { downloadToDelete = null },
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
                    text = if (item.isPlayableOffline) {
                        "Delete Download"
                    } else {
                        "Cancel Download"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniError
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AniBorder)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Are you sure you want to remove this episode from your local offline storage?",
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
                            .width(96.dp)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AniCardBackground)
                    ) {
                        AsyncImage(
                            model = item.posterUrl.ifBlank { item.coverUrl },
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
                            text = "Episode ${item.episode} • ${item.audioType.uppercase()} • ${item.qualityLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AniTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.formattedBytesDownloaded,
                            style = MaterialTheme.typography.labelSmall,
                            color = AniGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { downloadToDelete = null },
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
                            onCancelOrDeleteDownload(item.downloadId)
                            downloadToDelete = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        testTag = "confirm_delete_download_button"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearAllDownloadsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClearAllDownloadsSheet = false },
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
                    text = "Clear All Downloads",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniError
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AniBorder)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Are you sure you want to delete all downloaded anime episodes and clear the offline Media3 video cache?",
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
                        onClick = { showClearAllDownloadsSheet = false },
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
                        text = "Yes, Clear All",
                        onClick = {
                            onClearAllDownloads()
                            showClearAllDownloadsSheet = false
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
private fun StorageAndSettingsSummaryCard(
    usedBytes: Long,
    freeBytes: Long,
    completedCount: Int,
    wifiOnlyDownloads: Boolean,
    onToggleWifiOnly: (Boolean) -> Unit
) {
    val totalCapacity = (usedBytes + freeBytes).coerceAtLeast(1L)
    val usedFraction = (usedBytes.toFloat() / totalCapacity.toFloat()).coerceIn(0.02f, 1f)

    Surface(
        color = AniCardBackground,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("downloads_storage_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = AniGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Offline Storage • ${formatStorageBytes(usedBytes)} used",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AniTextPrimary
                    )
                }
                Text(
                    text = if (freeBytes > 0L) "${formatStorageBytes(freeBytes)} free" else "$completedCount episode(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AniTextSecondary
                )
            }

            LinearProgressIndicator(
                progress = { if (usedBytes == 0L) 0f else usedFraction },
                color = AniGreen,
                trackColor = AniSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = AniTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Wi-Fi Only Downloads",
                        style = MaterialTheme.typography.bodySmall,
                        color = AniTextSecondary
                    )
                }
                Switch(
                    checked = wifiOnlyDownloads,
                    onCheckedChange = onToggleWifiOnly,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AniGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = AniSurfaceElevated
                    ),
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("wifi_only_downloads_switch")
                )
            }
        }
    }
}

@Composable
private fun OfflineNoDownloadsEmptyState(
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = AniCardBackground,
            shape = CircleShape,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "No Downloads Available",
                    tint = AniGreen,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No Downloads Available",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AniTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "You're offline and haven't downloaded any episodes yet.\nConnect to the internet to download anime for offline watching.",
            style = MaterialTheme.typography.bodyLarge,
            color = AniTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        GreenPrimaryButton(
            text = "Retry Connection",
            onClick = onRetryConnection,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(50.dp),
            testTag = "retry_connection_button"
        )
    }
}

@Composable
private fun OfflineDownloadRowCard(
    item: OfflineDownloadItem,
    onPlayOffline: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDeleteOrCancel: () -> Unit
) {
    val state = item.state
    val isPlayable = item.isPlayableOffline

    Surface(
        color = AniCardBackground,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isPlayable) {
                onPlayOffline()
            }
            .testTag("download_item_${item.downloadId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rounded Anime Poster Thumbnail
                Box(
                    modifier = Modifier
                        .width(92.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AniSurfaceElevated)
                ) {
                    AsyncImage(
                        model = item.posterUrl.ifBlank { item.coverUrl },
                        contentDescription = item.animeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isPlayable) {
                        Surface(
                            color = AniGreen.copy(alpha = 0.92f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Offline",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = item.qualityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = AniGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Anime Title
                    Text(
                        text = item.animeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AniTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Episode Number + SUB/DUB + Quality Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AniSurfaceElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Episode ${item.episode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AniTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = AniGreenSurface,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.audioType.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = AniGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = AniSurfaceElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.qualityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = AniTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Status + Size Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isPlayable) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed Download",
                                    tint = AniGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (state == OfflineDownloadState.FAILED) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Failed Download",
                                    tint = AniError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (isPlayable) {
                                    "Downloaded • Ready Offline"
                                } else {
                                    "${state.displayLabel} (${String.format(Locale.US, "%.0f", item.progressPercent)}%)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isPlayable -> AniGreen
                                    state == OfflineDownloadState.FAILED -> AniError
                                    else -> AniTextPrimary
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = item.formattedBytesDownloaded,
                            style = MaterialTheme.typography.labelSmall,
                            color = AniTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Progress bar for active/paused/completed downloads
            if (state == OfflineDownloadState.DOWNLOADING ||
                state == OfflineDownloadState.QUEUED ||
                state == OfflineDownloadState.PAUSED ||
                state == OfflineDownloadState.COMPLETED
            ) {
                val progressFraction = if (isPlayable) {
                    1f
                } else {
                    (item.progressPercent / 100f).coerceIn(0f, 1f)
                }
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = AniGreen,
                    trackColor = AniSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            // Exact failure reason if proxy/segment download failed
            if (state == OfflineDownloadState.FAILED && !item.failureReasonText.isNullOrBlank()) {
                Surface(
                    color = AniError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.failureReasonText,
                        style = MaterialTheme.typography.bodySmall,
                        color = AniError,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Action Buttons Row (Play / Pause / Resume / Retry / Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state) {
                    OfflineDownloadState.DOWNLOADING,
                    OfflineDownloadState.QUEUED -> {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniSurfaceElevated,
                                contentColor = AniGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("pause_download_${item.downloadId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pause (${String.format(Locale.US, "%.0f", item.progressPercent)}%)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OfflineDownloadState.PAUSED -> {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniGreenSurface,
                                contentColor = AniGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("resume_download_${item.downloadId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Download", fontWeight = FontWeight.Bold)
                        }
                    }

                    OfflineDownloadState.FAILED -> {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniGreenSurface,
                                contentColor = AniGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("retry_download_${item.downloadId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Download", fontWeight = FontWeight.Bold)
                        }
                    }

                    OfflineDownloadState.COMPLETED -> {
                        Button(
                            onClick = onPlayOffline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("play_offline_${item.downloadId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Offline", fontWeight = FontWeight.Bold)
                        }
                    }

                    OfflineDownloadState.REMOVING -> Unit
                }

                IconButton(
                    onClick = onDeleteOrCancel,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AniSurfaceElevated)
                        .testTag("delete_download_${item.downloadId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete or Cancel Download",
                        tint = AniError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}

