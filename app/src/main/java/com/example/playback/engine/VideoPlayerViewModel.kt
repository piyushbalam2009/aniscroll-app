package com.example.playback.engine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.common.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.StreamResolution
import com.example.domain.model.SubtitleTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlayerUiState(
    val anilistId: Int,
    val episode: Int,
    val totalEpisodes: Int,
    val audioType: String,
    val animeTitle: String,
    val anime: Anime? = null,
    val isLoadingStream: Boolean = true,
    val streamResolution: StreamResolution? = null,
    val activeStreamUrl: String? = null,
    val usedFallbackUrl: Boolean = false,
    val errorMessage: String? = null,
    val savedPositionMs: Long = 0L,
    val selectedSubtitleTrack: SubtitleTrack? = null, // null = Subtitles Off
    val subtitleSyncOffsetMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val selectedQualityLabel: String = "Auto (1080p)",
    val maxVideoHeight: Int = Int.MAX_VALUE,
    val isScreenLocked: Boolean = false,
    val sleepTimerRemainingSec: Int? = null
)

class VideoPlayerViewModel(
    private val container: AppContainer,
    initialAnilistId: Int,
    initialEpisode: Int,
    initialAudioType: String,
    initialTitle: String,
    initialTotalEpisodes: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            anilistId = initialAnilistId,
            episode = initialEpisode,
            totalEpisodes = initialTotalEpisodes.coerceAtLeast(1),
            audioType = initialAudioType,
            animeTitle = initialTitle
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val preferences = container.preferencesDataStore.preferencesFlow
    val episodeProgressFlow = container.historyRepository.observeEpisodeProgress(initialAnilistId)

    private var sleepTimerJob: Job? = null

    init {
        viewModelScope.launch {
            val prefs = container.preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                playbackSpeed = prefs.defaultPlaybackSpeed,
                selectedQualityLabel = prefs.defaultVideoQuality
            )
            // Load anime metadata for poster/cover saving
            val detailsRes = container.animeRepository.getAnimeDetails(initialAnilistId)
            val animeObj = (detailsRes as? AppResult.Success)?.data
            if (animeObj != null) {
                _uiState.value = _uiState.value.copy(
                    anime = animeObj,
                    animeTitle = animeObj.displayTitle,
                    totalEpisodes = animeObj.availableEpisodeCount.coerceAtLeast(_uiState.value.totalEpisodes)
                )
            }
            resolveCurrentEpisodeStream(preserveCurrentPositionMs = null)
        }
    }

    fun resolveCurrentEpisodeStream(preserveCurrentPositionMs: Long? = null) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.value = current.copy(
                isLoadingStream = true,
                errorMessage = null,
                usedFallbackUrl = false
            )
            val prefs = container.preferencesDataStore.preferencesFlow.first()
            val savedPos = preserveCurrentPositionMs ?: if (prefs.rememberPlaybackPosition) {
                container.historyRepository.getSavedPositionMs(current.anilistId, current.episode)
            } else 0L

            when (
                val res = container.aniScrollRepository.resolveEpisodeStream(
                    anilistId = current.anilistId,
                    episode = current.episode,
                    audioType = current.audioType
                )
            ) {
                is AppResult.Success -> {
                    val resolution = res.data
                    val defaultSub = resolution.subtitles.firstOrNull {
                        it.lang.contains(prefs.defaultSubtitleLang, ignoreCase = true)
                    } ?: resolution.subtitles.firstOrNull { it.isDefault }
                        ?: resolution.subtitles.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        isLoadingStream = false,
                        streamResolution = resolution,
                        activeStreamUrl = resolution.hlsUrl,
                        savedPositionMs = savedPos,
                        selectedSubtitleTrack = defaultSub,
                        errorMessage = null
                    )
                }

                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingStream = false,
                        errorMessage = res.message
                    )
                }

                AppResult.Loading -> Unit
            }
        }
    }

    fun tryFallbackStreamOrReportError(playerErrorMsg: String, currentPositionMs: Long) {
        val current = _uiState.value
        val fallback = current.streamResolution?.fallbackDirectM3u8Url
        if (!current.usedFallbackUrl && !fallback.isNullOrBlank() && fallback != current.activeStreamUrl) {
            _uiState.value = current.copy(
                activeStreamUrl = fallback,
                usedFallbackUrl = true,
                savedPositionMs = currentPositionMs,
                errorMessage = null
            )
        } else {
            _uiState.value = current.copy(
                errorMessage = "Playback Error: $playerErrorMsg. Stream URL may have expired—tap Retry to resolve a fresh stream."
            )
        }
    }

    fun switchEpisode(newEpisode: Int) {
        if (newEpisode < 1 || newEpisode > _uiState.value.totalEpisodes) return
        _uiState.value = _uiState.value.copy(
            episode = newEpisode,
            savedPositionMs = 0L,
            streamResolution = null,
            activeStreamUrl = null
        )
        resolveCurrentEpisodeStream(preserveCurrentPositionMs = null)
    }

    fun switchAudioType(newAudioType: String, currentPositionMs: Long) {
        if (newAudioType.equals(_uiState.value.audioType, ignoreCase = true)) return
        _uiState.value = _uiState.value.copy(
            audioType = newAudioType.lowercase(),
            streamResolution = null,
            activeStreamUrl = null
        )
        resolveCurrentEpisodeStream(preserveCurrentPositionMs = currentPositionMs)
    }

    fun selectSubtitleTrack(track: SubtitleTrack?) {
        _uiState.value = _uiState.value.copy(selectedSubtitleTrack = track)
    }

    fun adjustSubtitleSyncOffset(deltaMs: Long) {
        _uiState.value = _uiState.value.copy(
            subtitleSyncOffsetMs = (_uiState.value.subtitleSyncOffsetMs + deltaMs).coerceIn(-15000L, 15000L)
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setVideoQuality(label: String, maxHeight: Int) {
        _uiState.value = _uiState.value.copy(
            selectedQualityLabel = label,
            maxVideoHeight = maxHeight
        )
    }

    fun toggleScreenLock() {
        _uiState.value = _uiState.value.copy(isScreenLocked = !_uiState.value.isScreenLocked)
    }

    fun setSleepTimerMinutes(minutes: Int?, onExpirePause: () -> Unit) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.value = _uiState.value.copy(sleepTimerRemainingSec = null)
            return
        }
        val totalSec = minutes * 60
        _uiState.value = _uiState.value.copy(sleepTimerRemainingSec = totalSec)
        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerRemainingSec = remaining)
            }
            _uiState.value = _uiState.value.copy(sleepTimerRemainingSec = null)
            onExpirePause()
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val state = _uiState.value
        val anime = state.anime
        viewModelScope.launch {
            container.historyRepository.savePlaybackProgress(
                anime = anime,
                anilistId = state.anilistId,
                animeTitle = anime?.displayTitle ?: state.animeTitle,
                posterUrl = anime?.posterUrl ?: "",
                coverUrl = anime?.coverUrl,
                episode = state.episode,
                totalEpisodes = state.totalEpisodes,
                audioType = state.audioType,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    companion object {
        fun provideFactory(
            container: AppContainer,
            anilistId: Int,
            episode: Int,
            audioType: String,
            title: String,
            totalEpisodes: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VideoPlayerViewModel(
                    container = container,
                    initialAnilistId = anilistId,
                    initialEpisode = episode,
                    initialAudioType = audioType,
                    initialTitle = title,
                    initialTotalEpisodes = totalEpisodes
                ) as T
            }
        }
    }
}
