package com.example.feature.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.AppResult
import com.example.core.designsystem.GreenOutlinedButton
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SelectablePillChip
import com.example.core.designsystem.SpinningDotsLoader
import com.example.domain.model.Anime
import com.example.domain.model.HlsPlaylistInspection
import com.example.domain.model.HlsQualityVariant
import com.example.domain.model.OfflineDownloadItem
import com.example.domain.model.OfflineDownloadState
import com.example.ui.theme.AniBorder
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniError
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniGreenSurface
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniSurfaceElevated
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HlsDownloadEpisodeSheet(
    anime: Anime,
    initialEpisode: Int,
    initialAudioType: String,
    existingDownloads: Map<Int, OfflineDownloadItem>,
    onInspectEpisodeDownload: suspend (Anime, Int, String) -> AppResult<HlsPlaylistInspection>,
    onStartEpisodeDownload: suspend (HlsPlaylistInspection, HlsQualityVariant) -> AppResult<OfflineDownloadItem>,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelOrDeleteDownload: (String) -> Unit,
    onPlayOfflineEpisode: (Int, String) -> Unit,
    onStreamOnlineEpisode: (Int, String) -> Unit,
    onOpenDownloadsScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val totalEpisodes = anime.availableEpisodeCount.coerceAtLeast(1)

    var selectedEpisode by remember(initialEpisode) {
        mutableIntStateOf(initialEpisode.coerceIn(1, totalEpisodes))
    }
    var selectedAudioType by remember(initialAudioType) {
        mutableStateOf(initialAudioType.lowercase())
    }
    var inspectionKey by remember { mutableIntStateOf(0) }
    var inspectionState by remember {
        mutableStateOf<AppResult<HlsPlaylistInspection>>(AppResult.Loading)
    }
    var selectedQualityLabel by remember { mutableStateOf<String?>(null) }
    var isStartingDownload by remember { mutableStateOf(false) }
    var startDownloadError by remember { mutableStateOf<String?>(null) }

    val activeDownloadForEpisode = existingDownloads[selectedEpisode]?.takeIf {
        it.audioType.equals(selectedAudioType, ignoreCase = true)
    } ?: existingDownloads[selectedEpisode]

    LaunchedEffect(anime.anilistId, selectedEpisode, selectedAudioType, inspectionKey) {
        inspectionState = AppResult.Loading
        startDownloadError = null
        val result = onInspectEpisodeDownload(anime, selectedEpisode, selectedAudioType)
        inspectionState = result
        if (result is AppResult.Success) {
            val available = result.data.variants.filter { it.isAvailable }
            selectedQualityLabel = available
                .firstOrNull { it.qualityLabel == "720p" }?.qualityLabel
                ?: available.lastOrNull()?.qualityLabel
        } else {
            selectedQualityLabel = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AniSecondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("hls_download_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline HLS Download",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AniTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${anime.displayTitle} • Episode $selectedEpisode",
                        style = MaterialTheme.typography.bodySmall,
                        color = AniTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AniTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AniBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // Audio Type Selector (SUB / DUB)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Audio Track:",
                    style = MaterialTheme.typography.labelLarge,
                    color = AniTextSecondary
                )
                SelectablePillChip(
                    text = "SUB (Japanese)",
                    selected = selectedAudioType == "sub",
                    onClick = { selectedAudioType = "sub" },
                    testTag = "download_audio_sub_chip"
                )
                SelectablePillChip(
                    text = "DUB (English)",
                    selected = selectedAudioType == "dub",
                    onClick = { selectedAudioType = "dub" },
                    testTag = "download_audio_dub_chip"
                )
            }

            // Episode Selector when series has multiple episodes
            if (totalEpisodes > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select Episode:",
                    style = MaterialTheme.typography.labelLarge,
                    color = AniTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val episodes = remember(totalEpisodes) { (1..totalEpisodes).toList() }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(episodes, key = { "dl_ep_$it" }) { ep ->
                        val isSelected = ep == selectedEpisode
                        val epDownload = existingDownloads[ep]
                        Surface(
                            color = when {
                                isSelected -> AniGreen
                                epDownload?.state == OfflineDownloadState.COMPLETED -> AniGreenSurface
                                else -> AniCardBackground
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected || epDownload != null) AniGreen else AniBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedEpisode = ep }
                                .testTag("download_ep_chip_$ep")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ep $ep",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else AniTextPrimary
                                )
                                if (epDownload?.state == OfflineDownloadState.COMPLETED) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = if (isSelected) Color.White else AniGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active or Completed Download Status Card for this Episode
            if (activeDownloadForEpisode != null) {
                ActiveEpisodeDownloadStatusCard(
                    item = activeDownloadForEpisode,
                    onPause = { onPauseDownload(activeDownloadForEpisode.downloadId) },
                    onResume = { onResumeDownload(activeDownloadForEpisode.downloadId) },
                    onCancelOrDelete = { onCancelOrDeleteDownload(activeDownloadForEpisode.downloadId) },
                    onPlayOffline = {
                        onDismiss()
                        onPlayOfflineEpisode(activeDownloadForEpisode.episode, activeDownloadForEpisode.audioType)
                    },
                    onOpenDownloadsScreen = {
                        onDismiss()
                        onOpenDownloadsScreen()
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Live HLS Manifest & Quality Variant Inspection Section
            when (val current = inspectionState) {
                is AppResult.Loading -> {
                    Surface(
                        color = AniCardBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AniBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SpinningDotsLoader(size = 38.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Inspecting AniScroll hlsProxyUrl & Master Playlist...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AniTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verifying #EXTM3U manifest, checking DRM status, and probing 360p / 480p / 720p / 1080p variants via Media3 DownloadHelper.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AniTextSecondary
                            )
                        }
                    }
                }

                is AppResult.Error -> {
                    HlsDownloadFailureNoticeCard(
                        failureMessage = current.message,
                        onRetryInspect = { inspectionKey++ },
                        onStreamOnline = {
                            onDismiss()
                            onStreamOnlineEpisode(selectedEpisode, selectedAudioType)
                        }
                    )
                }

                is AppResult.Success -> {
                    val inspection = current.data
                    val availableVariants = inspection.variants.filter { it.isAvailable }
                    val selectedVariant = inspection.variants.firstOrNull {
                        it.qualityLabel == selectedQualityLabel && it.isAvailable
                    }

                    // Verified Manifest Summary Badge
                    Surface(
                        color = AniCardBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AniBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AniGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Verified HLS Proxy Manifest (#EXTM3U)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AniGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { inspectionKey++ },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Re-inspect playlist",
                                        tint = AniTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Proxy Endpoint: ${inspection.hlsProxyUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AniTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (inspection.subtitles.isNotEmpty()) {
                                    "Subtitles included: ${inspection.subtitles.joinToString(", ") { it.lang }}"
                                } else {
                                    "Audio & video segments ready via Media3 SimpleCache"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = AniTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select Video Quality (Available in Master Playlist):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AniTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 360p, 480p, 720p, 1080p quality cards (selectable ONLY when available)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        inspection.variants.forEach { variant ->
                            val isSelected = variant.isAvailable && variant.qualityLabel == selectedQualityLabel
                            QualityVariantOptionRow(
                                variant = variant,
                                isSelected = isSelected,
                                onSelect = {
                                    if (variant.isAvailable) {
                                        selectedQualityLabel = variant.qualityLabel
                                    }
                                }
                            )
                        }
                    }

                    if (startDownloadError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HlsDownloadFailureNoticeCard(
                            failureMessage = startDownloadError ?: "Failed to queue HLS download.",
                            onRetryInspect = {
                                startDownloadError = null
                                inspectionKey++
                            },
                            onStreamOnline = {
                                onDismiss()
                                onStreamOnlineEpisode(selectedEpisode, selectedAudioType)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GreenPrimaryButton(
                        text = when {
                            isStartingDownload -> "Preparing Media3 DownloadHelper..."
                            selectedVariant != null -> "Download ${selectedVariant.qualityLabel} (${selectedAudioType.uppercase()})"
                            else -> "Select an Available Quality"
                        },
                        icon = Icons.Default.Download,
                        enabled = !isStartingDownload && selectedVariant != null && availableVariants.isNotEmpty(),
                        onClick = {
                            val chosen = selectedVariant ?: return@GreenPrimaryButton
                            isStartingDownload = true
                            startDownloadError = null
                            coroutineScope.launch {
                                val startResult = onStartEpisodeDownload(inspection, chosen)
                                isStartingDownload = false
                                if (startResult is AppResult.Error) {
                                    startDownloadError = startResult.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "confirm_start_hls_download_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GreenOutlinedButton(
                text = "Open Downloads Manager",
                onClick = {
                    onDismiss()
                    onOpenDownloadsScreen()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "open_downloads_manager_button"
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun QualityVariantOptionRow(
    variant: HlsQualityVariant,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = when {
        !variant.isAvailable -> AniCardBackground.copy(alpha = 0.45f)
        isSelected -> AniGreenSurface
        else -> AniCardBackground
    }
    val borderColor = when {
        !variant.isAvailable -> AniBorder.copy(alpha = 0.4f)
        isSelected -> AniGreen
        else -> AniBorder
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = variant.isAvailable, onClick = onSelect)
            .testTag("quality_variant_${variant.qualityLabel}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = variant.qualityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (variant.isAvailable) AniTextPrimary else AniTextSecondary.copy(alpha = 0.5f)
                    )
                    if (variant.isAvailable) {
                        Surface(
                            color = AniGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${variant.width}x${variant.height}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AniGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (variant.isAvailable) {
                        val codecsText = variant.codecs?.let { " • $it" }.orEmpty()
                        "Available in Master Playlist • ${variant.formattedBandwidth}$codecsText"
                    } else {
                        "Unavailable in Master Playlist"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (variant.isAvailable) AniTextSecondary else AniTextSecondary.copy(alpha = 0.45f)
                )
            }

            if (variant.isAvailable) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AniGreen else AniSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "N/A",
                    style = MaterialTheme.typography.labelSmall,
                    color = AniTextSecondary.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActiveEpisodeDownloadStatusCard(
    item: OfflineDownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancelOrDelete: () -> Unit,
    onPlayOffline: () -> Unit,
    onOpenDownloadsScreen: () -> Unit
) {
    Surface(
        color = AniSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            when (item.state) {
                OfflineDownloadState.FAILED -> AniError
                OfflineDownloadState.COMPLETED -> AniGreen
                else -> AniBorder
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ep ${item.episode} (${item.qualityLabel} • ${item.audioType.uppercase()})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
                Surface(
                    color = when (item.state) {
                        OfflineDownloadState.COMPLETED -> AniGreenSurface
                        OfflineDownloadState.FAILED -> AniError.copy(alpha = 0.16f)
                        else -> AniCardBackground
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.state.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (item.state) {
                            OfflineDownloadState.COMPLETED -> AniGreen
                            OfflineDownloadState.FAILED -> AniError
                            else -> AniTextPrimary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.state == OfflineDownloadState.DOWNLOADING ||
                item.state == OfflineDownloadState.QUEUED ||
                item.state == OfflineDownloadState.PAUSED ||
                item.state == OfflineDownloadState.COMPLETED
            ) {
                val fraction = (item.progressPercent / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { if (item.state == OfflineDownloadState.COMPLETED) 1f else fraction },
                    color = AniGreen,
                    trackColor = AniCardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", item.progressPercent.coerceIn(0f, 100f))}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AniGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.formattedBytesDownloaded,
                        style = MaterialTheme.typography.labelSmall,
                        color = AniTextSecondary
                    )
                }
            }

            if (item.state == OfflineDownloadState.FAILED && !item.failureReasonText.isNullOrBlank()) {
                Text(
                    text = item.failureReasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AniError
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (item.state) {
                    OfflineDownloadState.DOWNLOADING, OfflineDownloadState.QUEUED -> {
                        OutlinedButton(
                            onClick = onPause,
                            border = BorderStroke(1.dp, AniGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = AniGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", color = AniGreen)
                        }
                    }
                    OfflineDownloadState.PAUSED, OfflineDownloadState.FAILED -> {
                        OutlinedButton(
                            onClick = onResume,
                            border = BorderStroke(1.dp, AniGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AniGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", color = AniGreen)
                        }
                    }
                    OfflineDownloadState.COMPLETED -> {
                        Button(
                            onClick = onPlayOffline,
                            colors = ButtonDefaults.buttonColors(containerColor = AniGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Offline", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    OfflineDownloadState.REMOVING -> Unit
                }

                OutlinedButton(
                    onClick = onCancelOrDelete,
                    border = BorderStroke(1.dp, AniError),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AniError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.state == OfflineDownloadState.COMPLETED) "Delete" else "Cancel",
                        color = AniError
                    )
                }
            }
        }
    }
}

@Composable
private fun HlsDownloadFailureNoticeCard(
    failureMessage: String,
    onRetryInspect: () -> Unit,
    onStreamOnline: () -> Unit
) {
    Surface(
        color = AniError.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AniError.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hls_download_failure_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Download Failure",
                    tint = AniError,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "HLS Proxy Download Verification Failed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AniError
                )
            }
            Text(
                text = failureMessage,
                style = MaterialTheme.typography.bodySmall,
                color = AniTextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetryInspect,
                    border = BorderStroke(1.dp, AniGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = AniGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Check", color = AniGreen)
                }
                Button(
                    onClick = onStreamOnline,
                    colors = ButtonDefaults.buttonColors(containerColor = AniGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stream Live", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
