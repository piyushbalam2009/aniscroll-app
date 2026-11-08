package com.example.playback.controls

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.core.common.TimeUtils
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SelectablePillChip
import com.example.core.designsystem.SpinningDotsLoader
import com.example.domain.model.UserPreferencesState
import com.example.playback.engine.VideoPlayerViewModel
import com.example.ui.theme.AniBackground
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary
import kotlinx.coroutines.delay

private enum class PlayerSheetType {
    NONE, SPEED, SUBTITLES, QUALITY, AUDIO_SERVER, SLEEP_TIMER
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()
    val prefs by viewModel.preferences.collectAsState(initial = UserPreferencesState())
    val episodeProgressMap by viewModel.episodeProgressFlow.collectAsState(initial = emptyMap())

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(PlayerSheetType.NONE) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var activeCueText by remember { mutableStateOf("") }

    // Set landscape orientation on enter, restore portrait on exit
    DisposableEffect(isLandscape) {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Attach Player Listener for state, cues, auto-next, and error recovery
    DisposableEffect(exoPlayer, prefs.autoPlayNextEpisode) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    viewModel.saveProgress(exoPlayer.duration, exoPlayer.duration)
                    if (prefs.autoPlayNextEpisode && uiState.episode < uiState.totalEpisodes) {
                        viewModel.switchEpisode(uiState.episode + 1)
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && exoPlayer.duration > 0L) {
                    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                activeCueText = cueGroup.cues.joinToString("\n") { it.text ?: "" }.trim()
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                viewModel.tryFallbackStreamOrReportError(
                    playerErrorMsg = error.errorCodeName,
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Load or update HLS stream & selected subtitle configuration
    LaunchedEffect(uiState.activeStreamUrl, uiState.selectedSubtitleTrack) {
        val streamUrl = uiState.activeStreamUrl ?: return@LaunchedEffect
        val referer = uiState.streamResolution?.refererUrl ?: "https://megaplay.buzz/"

        val headers = mapOf(
            "Referer" to referer,
            "Origin" to referer.trimEnd('/'),
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
        )
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(streamUrl))
            .setMimeType(MimeTypes.APPLICATION_M3U8)

        val selectedSub = uiState.selectedSubtitleTrack
        if (selectedSub != null) {
            val mimeType = if (selectedSub.url.contains(".srt", ignoreCase = true)) {
                MimeTypes.APPLICATION_SUBRIP
            } else {
                MimeTypes.TEXT_VTT
            }
            val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(selectedSub.url))
                .setMimeType(mimeType)
                .setLanguage(selectedSub.lang)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
        }

        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())

        val resumePos = if (exoPlayer.currentPosition > 2000L && exoPlayer.currentMediaItem != null) {
            exoPlayer.currentPosition
        } else {
            uiState.savedPositionMs
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (resumePos > 0L) {
            exoPlayer.seekTo(resumePos)
        }
        exoPlayer.playWhenReady = true
    }

    // Apply playback speed & quality track constraints
    LaunchedEffect(uiState.playbackSpeed, uiState.maxVideoHeight) {
        exoPlayer.playbackParameters = PlaybackParameters(uiState.playbackSpeed)
        val params = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, uiState.maxVideoHeight)
            .build()
        exoPlayer.trackSelectionParameters = params
    }

    // Periodic progress tracker & 12-second Room auto-save
    LaunchedEffect(exoPlayer) {
        var tickCount = 0
        while (true) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0L) durationMs = dur
            tickCount++
            if (tickCount % 24 == 0 && exoPlayer.isPlaying && durationMs > 0L) {
                viewModel.saveProgress(currentPositionMs, durationMs)
            }
            delay(500L)
        }
    }

    BackHandler {
        if (showEpisodeDrawer) {
            showEpisodeDrawer = false
        } else {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("video_player_screen")
    ) {
        // Media3 PlayerView Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    subtitleView?.alpha = 0f // We render custom styled subtitle overlay below
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Styled Subtitle Overlay
        if (uiState.selectedSubtitleTrack != null && activeCueText.isNotBlank()) {
            val subColor = remember(prefs.subtitleColorHex) {
                runCatching { Color(android.graphics.Color.parseColor(prefs.subtitleColorHex)) }
                    .getOrDefault(Color.White)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = if (showControls) 96.dp else 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = prefs.subtitleBackgroundOpacity))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeCueText,
                    color = subColor,
                    fontSize = prefs.subtitleFontSizeSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Loading / Buffering Spinner
        if (uiState.isLoadingStream || isBuffering) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                SpinningDotsLoader(size = 54.dp)
            }
        }

        // Error State with Retry & Audio Switch
        if (uiState.errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AniSecondaryBackground.copy(alpha = 0.95f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Stream Resolution Notice",
                    style = MaterialTheme.typography.titleLarge,
                    color = AniTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "Unable to play stream.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AniTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GreenPrimaryButton(
                        text = "Retry Stream",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.resolveCurrentEpisodeStream(currentPositionMs) },
                        testTag = "player_retry_button"
                    )
                    val altAudio = if (uiState.audioType == "sub") "dub" else "sub"
                    SelectablePillChip(
                        text = "Try ${altAudio.uppercase()}",
                        selected = false,
                        onClick = { viewModel.switchAudioType(altAudio, currentPositionMs) }
                    )
                }
            }
        }

        // Player Controls Overlay matching Screenshots 40, 41, 43
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isScreenLocked) {
                // Locked Mode: Only show Unlock button at bottom-left
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { viewModel.toggleScreenLock() },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                            .clip(CircleShape)
                            .background(AniGreen)
                            .testTag("player_unlock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock Screen",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.72f),
                                    0.35f to Color.Transparent,
                                    0.65f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                ) {
                    // Top Controls Row (Back + Episode Title + Speed, Timer, Subtitles, Episodes, PiP, More)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                                    onBack()
                                },
                                modifier = Modifier.testTag("player_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = uiState.animeTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Episode ${uiState.episode} • ${uiState.audioType.uppercase()} • ${uiState.selectedQualityLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AniGreen
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { activeSheet = PlayerSheetType.SPEED },
                                modifier = Modifier.testTag("player_speed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback Speed",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { activeSheet = PlayerSheetType.SLEEP_TIMER },
                                modifier = Modifier.testTag("player_timer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (uiState.sleepTimerRemainingSec != null) AniGreen else Color.White
                                )
                            }
                            IconButton(
                                onClick = { activeSheet = PlayerSheetType.SUBTITLES },
                                modifier = Modifier.testTag("player_subtitles_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (uiState.selectedSubtitleTrack != null) AniGreen else Color.White
                                )
                            }
                            IconButton(
                                onClick = { showEpisodeDrawer = !showEpisodeDrawer },
                                modifier = Modifier.testTag("player_episodes_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Episodes",
                                    tint = if (showEpisodeDrawer) AniGreen else Color.White
                                )
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                IconButton(
                                    onClick = {
                                        runCatching {
                                            val params = PictureInPictureParams.Builder()
                                                .setAspectRatio(Rational(16, 9))
                                                .build()
                                            activity.enterPictureInPictureMode(params)
                                        }
                                    },
                                    modifier = Modifier.testTag("player_pip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPictureAlt,
                                        contentDescription = "Picture in Picture",
                                        tint = Color.White
                                    )
                                }
                            }
                            IconButton(
                                onClick = { activeSheet = PlayerSheetType.AUDIO_SERVER },
                                modifier = Modifier.testTag("player_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Audio & Server Settings",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Bottom Seek Bar + Transport Controls matching Screenshots 40 & 43
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = TimeUtils.formatDurationMs(currentPositionMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Slider(
                                value = if (durationMs > 0L) {
                                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f,
                                onValueChange = { fraction ->
                                    if (durationMs > 0L) {
                                        val seekTarget = (fraction * durationMs).toLong()
                                        currentPositionMs = seekTarget
                                        exoPlayer.seekTo(seekTarget)
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = AniGreen,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_seek_slider")
                            )
                            Text(
                                text = TimeUtils.formatDurationMs(durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Lock & Mute
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleScreenLock() },
                                    modifier = Modifier.testTag("player_lock_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Lock Screen",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        exoPlayer.volume = if (isMuted) 0f else 1f
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Center: Rewind 10s, Prev Ep, Play/Pause, Next Ep, Forward 10s
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                    },
                                    modifier = Modifier.testTag("player_rewind_10")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    enabled = uiState.episode > 1,
                                    onClick = { viewModel.switchEpisode(uiState.episode - 1) },
                                    modifier = Modifier.testTag("player_prev_episode")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Episode",
                                        tint = if (uiState.episode > 1) Color.White else Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clickable {
                                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        }
                                        .testTag("player_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = AniBackground,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                IconButton(
                                    enabled = uiState.episode < uiState.totalEpisodes,
                                    onClick = { viewModel.switchEpisode(uiState.episode + 1) },
                                    modifier = Modifier.testTag("player_next_episode")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Episode",
                                        tint = if (uiState.episode < uiState.totalEpisodes) Color.White else Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(10_000L)))
                                    },
                                    modifier = Modifier.testTag("player_forward_10")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Right: Quality + Fullscreen orientation toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { activeSheet = PlayerSheetType.QUALITY },
                                    modifier = Modifier.testTag("player_quality_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Video Quality",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { isLandscape = !isLandscape },
                                    modifier = Modifier.testTag("player_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Orientation",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right-side Episode Drawer matching Screenshot 41_Dark_anime episode details, play episode option
        AnimatedVisibility(
            visible = showEpisodeDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(290.dp)
                    .fillMaxHeight()
                    .background(AniSecondaryBackground.copy(alpha = 0.96f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { showEpisodeDrawer = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close Episodes",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Episode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val episodesList = (1..uiState.totalEpisodes).toList()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(episodesList, key = { "drawer_ep_$it" }) { epNum ->
                        val isCurrent = epNum == uiState.episode
                        val prog = episodeProgressMap[epNum]?.progressFraction
                            ?: if (isCurrent && durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()) else 0f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) AniCardBackground else Color.Transparent)
                                .clickable {
                                    viewModel.switchEpisode(epNum)
                                    showEpisodeDrawer = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episode $epNum",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) AniGreen else Color.White
                                )
                                Text(
                                    text = "24:20",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AniTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { prog.coerceIn(0f, 1f) },
                                color = AniGreen,
                                trackColor = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings Bottom Sheets for Speed, Subtitles, Quality, Audio/Server, Sleep Timer
    if (activeSheet != PlayerSheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = PlayerSheetType.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AniSecondaryBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeSheet) {
                    PlayerSheetType.SPEED -> {
                        Text(
                            text = "Playback Speed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            speeds.forEach { spd ->
                                SelectablePillChip(
                                    text = "${spd}x",
                                    selected = uiState.playbackSpeed == spd,
                                    onClick = {
                                        viewModel.setPlaybackSpeed(spd)
                                        activeSheet = PlayerSheetType.NONE
                                    }
                                )
                            }
                        }
                    }

                    PlayerSheetType.SUBTITLES -> {
                        Text(
                            text = "Subtitles & Captions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        val tracks = uiState.streamResolution?.subtitles.orEmpty()
                        SelectablePillChip(
                            text = "Subtitles Off",
                            selected = uiState.selectedSubtitleTrack == null,
                            onClick = {
                                viewModel.selectSubtitleTrack(null)
                                activeSheet = PlayerSheetType.NONE
                            }
                        )
                        if (tracks.isEmpty()) {
                            Text(
                                text = "No external subtitle tracks reported by provider for this stream.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AniTextSecondary
                            )
                        } else {
                            tracks.forEach { track ->
                                SelectablePillChip(
                                    text = track.lang,
                                    selected = uiState.selectedSubtitleTrack?.url == track.url,
                                    onClick = {
                                        viewModel.selectSubtitleTrack(track)
                                        activeSheet = PlayerSheetType.NONE
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Sync Offset: ${uiState.subtitleSyncOffsetMs} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = AniTextSecondary
                            )
                            SelectablePillChip(
                                text = "-250ms",
                                selected = false,
                                onClick = { viewModel.adjustSubtitleSyncOffset(-250L) }
                            )
                            SelectablePillChip(
                                text = "+250ms",
                                selected = false,
                                onClick = { viewModel.adjustSubtitleSyncOffset(250L) }
                            )
                        }
                    }

                    PlayerSheetType.QUALITY -> {
                        Text(
                            text = "Video Quality",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        val qualities = listOf(
                            "Auto (1080p)" to Int.MAX_VALUE,
                            "1080p Full HD" to 1080,
                            "720p HD" to 720,
                            "480p SD" to 480,
                            "360p Data Saver" to 360
                        )
                        qualities.forEach { (label, maxH) ->
                            SelectablePillChip(
                                text = label,
                                selected = uiState.selectedQualityLabel == label,
                                onClick = {
                                    viewModel.setVideoQuality(label, maxH)
                                    activeSheet = PlayerSheetType.NONE
                                }
                            )
                        }
                    }

                    PlayerSheetType.AUDIO_SERVER -> {
                        Text(
                            text = "Audio & Streaming Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SelectablePillChip(
                                text = "SUB (Japanese)",
                                selected = uiState.audioType == "sub",
                                onClick = {
                                    viewModel.switchAudioType("sub", currentPositionMs)
                                    activeSheet = PlayerSheetType.NONE
                                }
                            )
                            SelectablePillChip(
                                text = "DUB (English)",
                                selected = uiState.audioType == "dub",
                                onClick = {
                                    viewModel.switchAudioType("dub", currentPositionMs)
                                    activeSheet = PlayerSheetType.NONE
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Active Server: ${uiState.streamResolution?.server ?: "MegaPlay Direct"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AniGreen
                        )
                    }

                    PlayerSheetType.SLEEP_TIMER -> {
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AniTextPrimary
                        )
                        val options = listOf(null to "Off", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (mins, label) ->
                                SelectablePillChip(
                                    text = label,
                                    selected = (mins == null && uiState.sleepTimerRemainingSec == null),
                                    onClick = {
                                        viewModel.setSleepTimerMinutes(mins) {
                                            exoPlayer.pause()
                                        }
                                        activeSheet = PlayerSheetType.NONE
                                    }
                                )
                            }
                        }
                    }

                    PlayerSheetType.NONE -> Unit
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
