package com.example.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.AniScrollLogo
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SelectablePillChip
import com.example.domain.model.UserPreferencesState
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
fun SettingsScreen(
    preferences: UserPreferencesState,
    cacheSizeText: String,
    downloadedEpisodesCount: Int = 0,
    onOpenDownloads: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenInterests: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetDefaultSubtitle: (String) -> Unit,
    onSetDefaultAudio: (String) -> Unit,
    onSetDefaultSpeed: (Float) -> Unit,
    onSetDefaultQuality: (String) -> Unit,
    onSetDefaultDisplayMode: (com.example.domain.model.VideoDisplayMode) -> Unit = {},
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetRememberPosition: (Boolean) -> Unit,
    onSetWifiOnlyDownloads: (Boolean) -> Unit = {},
    onUpdateNotificationPrefs: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onClearAllData: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLanguageModal by remember { mutableStateOf(false) }
    var showNotifModal by remember { mutableStateOf(false) }
    var showSpeedModal by remember { mutableStateOf(false) }
    var showQualityModal by remember { mutableStateOf(false) }
    var showDisplayModeModal by remember { mutableStateOf(false) }
    var showSubtitleModal by remember { mutableStateOf(false) }
    var showClearConfirmModal by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var importInput by remember { mutableStateOf("") }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AniScrollLogo(size = 30.dp)
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        statusBanner?.let { msg ->
            Surface(
                color = AniGreenSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = AniGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SettingsGroupHeader("Offline & Downloads")

            SettingsNavRow(
                icon = Icons.Default.Download,
                title = "Downloads",
                subtitle = "Manage downloaded anime and offline episodes",
                valueText = if (downloadedEpisodesCount > 0) "$downloadedEpisodesCount saved" else null,
                onClick = onOpenDownloads,
                testTag = "settings_downloads_row"
            )
            SettingsToggleRow(
                icon = Icons.Default.Download,
                title = "Wi-Fi Only Downloads",
                checked = preferences.wifiOnlyDownloads,
                onCheckedChange = onSetWifiOnlyDownloads
            )

            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroupHeader("General")

            SettingsToggleRow(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                checked = preferences.darkTheme,
                onCheckedChange = onSetDarkTheme
            )
            SettingsNavRow(
                icon = Icons.Default.Language,
                title = "Language",
                valueText = preferences.appLanguage,
                onClick = { showLanguageModal = true }
            )
            SettingsNavRow(
                icon = Icons.Default.ClosedCaption,
                title = "Default Subtitle Language",
                valueText = preferences.defaultSubtitleLang,
                onClick = { showSubtitleModal = true }
            )
            SettingsNavRow(
                icon = Icons.Default.Speed,
                title = "Default Playback Speed",
                valueText = "${preferences.defaultPlaybackSpeed}x",
                onClick = { showSpeedModal = true }
            )
            SettingsNavRow(
                icon = Icons.Default.HighQuality,
                title = "Default Video Quality",
                valueText = preferences.defaultVideoQuality,
                onClick = { showQualityModal = true }
            )
            SettingsNavRow(
                icon = Icons.Default.PlayCircleOutline,
                title = "Default Screen Size",
                valueText = preferences.defaultDisplayMode.displayName,
                onClick = { showDisplayModeModal = true }
            )
            SettingsNavRow(
                icon = Icons.Default.PlayCircleOutline,
                title = "Preferred Anime Genres",
                valueText = "${preferences.selectedInterests.size} selected",
                onClick = onOpenInterests
            )

            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroupHeader("Notifications & Playback")

            SettingsNavRow(
                icon = Icons.Default.NotificationsNone,
                title = "Notification Preferences",
                valueText = if (preferences.generalNotifications) "Enabled" else "Muted",
                onClick = { showNotifModal = true }
            )
            SettingsToggleRow(
                icon = Icons.Default.PlayCircleOutline,
                title = "Auto Play Next Episode",
                checked = preferences.autoPlayNextEpisode,
                onCheckedChange = onSetAutoPlayNext
            )
            SettingsNavRow(
                icon = Icons.Default.ClosedCaption,
                title = "Default Audio Stream",
                valueText = preferences.defaultAudioType.uppercase(),
                onClick = {
                    onSetDefaultAudio(if (preferences.defaultAudioType == "sub") "dub" else "sub")
                }
            )
            SettingsToggleRow(
                icon = Icons.Default.PlayCircleOutline,
                title = "Remember Playback Position",
                checked = preferences.rememberPlaybackPosition,
                onCheckedChange = onSetRememberPosition
            )

            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroupHeader("Storage & Local Backup")

            SettingsNavRow(
                icon = Icons.Default.CleaningServices,
                title = "Clear Cache",
                subtitle = "Clears image & metadata cache only (keeps downloaded episodes)",
                valueText = cacheSizeText,
                onClick = {
                    onClearCache()
                    statusBanner = "Image and metadata cache cleared. Offline downloads preserved."
                }
            )
            SettingsNavRow(
                icon = Icons.Default.FileUpload,
                title = "Export Local Data (JSON)",
                valueText = "Backup",
                onClick = {
                    scope.launch {
                        val json = onExportJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AniScroll Backup", json))
                        statusBanner = "Local watchlist & history JSON copied to clipboard!"
                    }
                }
            )
            SettingsNavRow(
                icon = Icons.Default.FileDownload,
                title = "Import Local Data (JSON)",
                valueText = "Restore",
                onClick = { showImportDialog = true }
            )
            SettingsNavRow(
                icon = Icons.Default.DeleteForever,
                title = "Clear All Local Data",
                valueText = "Reset",
                tint = AniError,
                onClick = { showClearConfirmModal = true }
            )

            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroupHeader("Privacy & Help")

            SettingsNavRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Help Center & FAQ",
                onClick = onOpenHelpCenter
            )
            SettingsNavRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy & Data Storage",
                onClick = onOpenPrivacyPolicy
            )
            SettingsNavRow(
                icon = Icons.Default.Info,
                title = "About AniScroll & Licenses",
                valueText = "v1.0.0",
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Language Selection Sheet matching Screenshot 60_Dark_settings language
    if (showLanguageModal) {
        val languages = listOf(
            "English (US)", "English (UK)", "Japanese", "Hindi",
            "Mandarin", "Spanish", "French", "Arabic", "Bengali", "Russian", "Indonesia"
        )
        ModalBottomSheet(
            onDismissRequest = { showLanguageModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Language",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetLanguage(lang)
                                showLanguageModal = false
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AniTextPrimary
                        )
                        RadioButton(
                            selected = preferences.appLanguage == lang,
                            onClick = {
                                onSetLanguage(lang)
                                showLanguageModal = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AniGreen,
                                unselectedColor = AniGreen
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Notification toggles sheet matching Screenshot 57_Dark_settings notification
    if (showNotifModal) {
        ModalBottomSheet(
            onDismissRequest = { showNotifModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Notification Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
                SettingsToggleRow(
                    icon = Icons.Default.NotificationsNone,
                    title = "General Notification",
                    checked = preferences.generalNotifications,
                    onCheckedChange = { onUpdateNotificationPrefs(it, null, null, null) }
                )
                SettingsToggleRow(
                    icon = Icons.Default.NotificationsNone,
                    title = "Airing Reminders",
                    checked = preferences.airingReminders,
                    onCheckedChange = { onUpdateNotificationPrefs(null, it, null, null) }
                )
                SettingsToggleRow(
                    icon = Icons.Default.NotificationsNone,
                    title = "New Episode Releases",
                    checked = preferences.newEpisodeReminders,
                    onCheckedChange = { onUpdateNotificationPrefs(null, null, it, null) }
                )
                SettingsToggleRow(
                    icon = Icons.Default.NotificationsNone,
                    title = "App Updates",
                    checked = preferences.appUpdateNotifications,
                    onCheckedChange = { onUpdateNotificationPrefs(null, null, null, it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSpeedModal) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        ModalBottomSheet(
            onDismissRequest = { showSpeedModal = false },
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Default Playback Speed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
                speeds.forEach { spd ->
                    SelectablePillChip(
                        text = "${spd}x",
                        selected = preferences.defaultPlaybackSpeed == spd,
                        onClick = {
                            onSetDefaultSpeed(spd)
                            showSpeedModal = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showQualityModal) {
        val qualities = listOf("Auto (1080p)", "1080p Full HD", "720p HD", "480p SD", "360p Data Saver")
        ModalBottomSheet(
            onDismissRequest = { showQualityModal = false },
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Default Video Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
                qualities.forEach { q ->
                    SelectablePillChip(
                        text = q,
                        selected = preferences.defaultVideoQuality == q,
                        onClick = {
                            onSetDefaultQuality(q)
                            showQualityModal = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDisplayModeModal) {
        ModalBottomSheet(
            onDismissRequest = { showDisplayModeModal = false },
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCREEN SIZE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AniTextPrimary
                    )
                    TextButton(
                        onClick = {
                            onSetDefaultDisplayMode(com.example.domain.model.VideoDisplayMode.FIT)
                            showDisplayModeModal = false
                        }
                    ) {
                        Text(
                            text = "Reset to Fit Screen",
                            color = AniGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                com.example.domain.model.VideoDisplayMode.entries.forEach { mode ->
                    SelectablePillChip(
                        text = mode.displayName,
                        selected = preferences.defaultDisplayMode == mode,
                        onClick = {
                            onSetDefaultDisplayMode(mode)
                            showDisplayModeModal = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSubtitleModal) {
        val subs = listOf("English", "Spanish", "Portuguese", "French", "German", "Arabic", "Indonesian", "Off")
        ModalBottomSheet(
            onDismissRequest = { showSubtitleModal = false },
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Default Subtitle Language",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AniTextPrimary
                )
                subs.forEach { lang ->
                    SelectablePillChip(
                        text = lang,
                        selected = preferences.defaultSubtitleLang == lang,
                        onClick = {
                            onSetDefaultSubtitle(lang)
                            showSubtitleModal = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearConfirmModal) {
        ModalBottomSheet(
            onDismissRequest = { showClearConfirmModal = false },
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Clear Local Data",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AniError
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AniBorder)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Are you sure you want to delete your local watchlist, favorites, ratings, and watch history?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AniTextPrimary
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showClearConfirmModal = false },
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
                            onClearAllData()
                            showClearConfirmModal = false
                            statusBanner = "All local user data cleared."
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

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = AniSecondaryBackground,
            title = { Text("Import Local Data JSON", color = AniTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = importInput,
                    onValueChange = { importInput = it },
                    placeholder = { Text("Paste exported AniScroll JSON here...", color = AniTextSecondary) },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = onImportJson(importInput)
                            statusBanner = if (ok) "Local data imported successfully!" else "Invalid backup JSON format."
                            showImportDialog = false
                            importInput = ""
                        }
                    }
                ) {
                    Text("Import", color = AniGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = AniTextSecondary)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = AniSecondaryBackground,
            title = { Text("AniScroll v1.0.0", color = AniTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "AniScroll is a privacy-first native Android anime discovery and HLS streaming application.\n\n• Metadata: AniList GraphQL API\n• Artwork: Kitsu JSON:API\n• Streaming: AniScroll HLS API\n• Playback & Offline HLS: AndroidX Media3 ExoPlayer & DownloadManager\n• Storage: 100% Local Room & DataStore (Zero cloud accounts or tracking)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AniTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = AniGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = AniGreen,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    valueText: String? = null,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tint,
                    fontWeight = FontWeight.Medium
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AniTextSecondary
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (valueText != null) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AniTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tint
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AniGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AniCardBackground
            )
        )
    }
}

@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("General") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedIndex by remember { mutableIntStateOf(0) }

    val faqs = listOf(
        Triple("General", "What is AniScroll?", "AniScroll is a native Android anime discovery, schedule tracking, and HLS streaming application built with AniList metadata, Kitsu artwork, and Media3 ExoPlayer."),
        Triple("General", "Do I need an account or login?", "No! AniScroll requires zero registration, login, or cloud accounts. Your watchlist, watch history, playback progress, and ratings are stored 100% locally on your device."),
        Triple("Service", "How do I remove anime from My List?", "Open the My List tab or the Anime Details screen, tap the bookmark or options menu icon on the anime card, and tap Remove from My List."),
        Triple("Video", "How do I switch between SUB and DUB?", "You can select SUB or DUB directly on the Anime Details screen before pressing Play, or switch live inside the Video Player's Audio & Server menu."),
        Triple("Video", "How do offline HLS downloads work?", "Tap the download icon next to any episode on the Anime Details screen to inspect available 360p, 480p, 720p, or 1080p HLS streams and save them for offline playback in the Downloads screen.")
    )

    val filteredFaqs = faqs.filter { (cat, q, a) ->
        (selectedCategory == "General" || cat == selectedCategory) &&
            (searchQuery.isBlank() || q.contains(searchQuery, true) || a.contains(searchQuery, true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("help_center_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("help_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Help Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // FAQ / Contact us Tabs matching Screenshots 62, 63, 64
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
                        text = "FAQ",
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
                        text = "Contact us",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) AniGreen else AniTextSecondary
                    )
                }
            )
        }

        if (selectedTab == 0) {
            val cats = listOf("General", "Service", "Video")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cats) { cat ->
                    SelectablePillChip(
                        text = cat,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = AniGreen
                    )
                },
                placeholder = { Text("Search FAQ...", color = AniTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AniGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = AniCardBackground,
                    unfocusedContainerColor = AniCardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredFaqs.size) { index ->
                    val (_, question, answer) = filteredFaqs[index]
                    val isExpanded = expandedIndex == index
                    Surface(
                        color = AniCardBackground,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedIndex = if (isExpanded) -1 else index }
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AniTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AniGreen
                                )
                            }
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = AniBorder)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AniTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Contact us cards matching Screenshot 64_Dark_settings help center contact us
            val contacts = listOf(
                Triple("AniScroll API Portal", "https://api.aniscroll.in", Icons.Default.SupportAgent),
                Triple("AniList Documentation", "https://docs.anilist.co", Icons.Default.Language),
                Triple("Kitsu API Reference", "https://kitsu.docs.apiary.io", Icons.Default.Info)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contacts) { (label, url, icon) ->
                    Surface(
                        color = AniCardBackground,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AniGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AniTextPrimary
                                )
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AniTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("privacy_policy_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "1. Local-Only Personal Data Storage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "AniScroll operates without user registration, login, email authentication, or cloud user profiles. Your watchlist, favorites, watch history, playback timestamps, recent searches, and personal anime ratings are stored strictly locally on your device using Android Jetpack Room and DataStore.",
                style = MaterialTheme.typography.bodyMedium,
                color = AniTextSecondary
            )

            Text(
                text = "2. External API Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "To display anime metadata, artwork, airing schedules, and video streams, the app makes direct HTTPS requests to:\n• AniList GraphQL API (https://graphql.anilist.co)\n• Kitsu JSON:API (https://kitsu.io/api/edge)\n• AniScroll Streaming API (https://api.aniscroll.in)\n\nNo personal identifiers or credentials are transmitted with these requests.",
                style = MaterialTheme.typography.bodyMedium,
                color = AniTextSecondary
            )

            Text(
                text = "3. User Control & Data Portability",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "You have complete control over your on-device data at all times. From Settings, you can export your local data as a portable JSON backup, restore from a JSON backup, clear cached artwork and metadata, or wipe all local watch history and lists.",
                style = MaterialTheme.typography.bodyMedium,
                color = AniTextSecondary
            )
        }
    }
}
