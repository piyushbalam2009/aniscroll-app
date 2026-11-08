package com.example.feature.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.common.AppResult
import com.example.core.network.NetworkConnectionState
import com.example.domain.model.AiringScheduleItem
import com.example.domain.model.Anime
import com.example.domain.model.AnimeFilterState
import com.example.domain.model.HlsPlaylistInspection
import com.example.domain.model.HlsQualityVariant
import com.example.domain.model.OfflineDownloadItem
import com.example.domain.model.UserPreferencesState
import com.example.domain.model.WatchHistoryItem
import com.example.domain.model.WatchStatus
import com.example.domain.model.WatchlistItem
import com.example.domain.repository.HomeDashboardData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class HomeUiState(
    val isLoading: Boolean = true,
    val dashboard: HomeDashboardData = HomeDashboardData(),
    val errorMessage: String? = null
)

data class SearchUiState(
    val query: String = "",
    val filter: AnimeFilterState = AnimeFilterState(),
    val isSearching: Boolean = false,
    val results: List<Anime> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
class MainViewModel(
    private val container: AppContainer
) : ViewModel() {

    val preferences: StateFlow<UserPreferencesState> = container.preferencesDataStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferencesState()
        )

    val connectionState: StateFlow<NetworkConnectionState> = container.networkConnectivityObserver.connectionState
    val isOnline: StateFlow<Boolean> = container.networkConnectivityObserver.isOnline

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady.asStateFlow()

    // Home Dashboard State
    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    // Top Hits & New Releases full lists
    private val _topHitsState = MutableStateFlow<AppResult<List<Anime>>>(AppResult.Loading)
    val topHitsState: StateFlow<AppResult<List<Anime>>> = _topHitsState.asStateFlow()

    private val _newReleasesState = MutableStateFlow<AppResult<List<Anime>>>(AppResult.Loading)
    val newReleasesState: StateFlow<AppResult<List<Anime>>> = _newReleasesState.asStateFlow()

    // Search & Filter State
    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()
    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    val recentSearches: StateFlow<List<String>> = container.animeRepository.observeRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Watchlist & Favorites
    val watchlist: StateFlow<List<WatchlistItem>> = container.watchlistRepository.observeWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistIds: StateFlow<Set<Int>> = container.watchlistRepository.observeWatchlistIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // History & Continue Watching
    val watchHistory: StateFlow<List<WatchHistoryItem>> = container.historyRepository.observeWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<WatchHistoryItem>> = container.historyRepository.observeContinueWatching()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Offline HLS Downloads
    val offlineDownloads: StateFlow<List<OfflineDownloadItem>> = container.offlineDownloadRepository.observeAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Details Cache Map
    private val _animeDetailsMap = MutableStateFlow<Map<Int, AppResult<Anime>>>(emptyMap())
    val animeDetailsMap: StateFlow<Map<Int, AppResult<Anime>>> = _animeDetailsMap.asStateFlow()

    // Release Schedule State
    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset: StateFlow<Int> = _selectedDayOffset.asStateFlow()

    private val _scheduleState = MutableStateFlow<AppResult<List<AiringScheduleItem>>>(AppResult.Loading)
    val scheduleState: StateFlow<AppResult<List<AiringScheduleItem>>> = _scheduleState.asStateFlow()

    val scheduledReminderIds: StateFlow<Set<Int>> = container.notificationScheduler.scheduledIds

    init {
        viewModelScope.launch {
            val initialPrefs = container.preferencesDataStore.preferencesFlow.first()
            container.media3DownloadStack.setWifiOnlyRequirements(initialPrefs.wifiOnlyDownloads)
            _isAppReady.value = true
            if (isOnline.value) {
                refreshHome(initialPrefs.selectedInterests)
                loadTopHits()
                loadNewReleases()
                loadScheduleForDayOffset(0)
            }
        }

        // Automatically load Home data once when device transitions from Offline -> Online
        viewModelScope.launch {
            isOnline.collectLatest { online ->
                if (online && _isAppReady.value && _homeState.value.dashboard.trendingAnime.isEmpty()) {
                    refreshHome(preferences.value.selectedInterests)
                    loadTopHits()
                    loadNewReleases()
                    loadScheduleForDayOffset(_selectedDayOffset.value)
                }
            }
        }

        // Debounced search listener
        viewModelScope.launch {
            searchQueryFlow
                .debounce(420L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val currentFilter = _searchState.value.filter.copy(query = query)
                    val hasActiveFilters = query.isNotBlank() ||
                        currentFilter.format != null ||
                        currentFilter.countryOfOrigin != null ||
                        currentFilter.genres.isNotEmpty() ||
                        currentFilter.year != null ||
                        currentFilter.season != null ||
                        currentFilter.status != null

                    if (hasActiveFilters) {
                        executeSearch(currentFilter, saveToHistory = query.length >= 3)
                    } else {
                        _searchState.value = _searchState.value.copy(
                            isSearching = false,
                            hasSearched = false,
                            results = emptyList(),
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun refreshHome(interests: Set<String> = preferences.value.selectedInterests) {
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true, errorMessage = null)
            val data = container.animeRepository.getHomeDashboard(interests)
            _homeState.value = HomeUiState(
                isLoading = false,
                dashboard = data,
                errorMessage = data.errorMessage
            )
        }
    }

    fun loadTopHits() {
        viewModelScope.launch {
            _topHitsState.value = AppResult.Loading
            _topHitsState.value = container.animeRepository.getTopHits(1)
        }
    }

    fun loadNewReleases() {
        viewModelScope.launch {
            _newReleasesState.value = AppResult.Loading
            _newReleasesState.value = container.animeRepository.getNewReleases(1)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchState.value = _searchState.value.copy(
            query = query,
            filter = _searchState.value.filter.copy(query = query)
        )
        searchQueryFlow.value = query
    }

    fun applyFilterState(newFilter: AnimeFilterState) {
        val merged = newFilter.copy(query = _searchState.value.query)
        _searchState.value = _searchState.value.copy(filter = merged)
        executeSearch(merged, saveToHistory = false)
    }

    fun resetFilterState() {
        val reset = AnimeFilterState(query = _searchState.value.query)
        _searchState.value = _searchState.value.copy(filter = reset)
        if (reset.query.isNotBlank()) {
            executeSearch(reset, saveToHistory = false)
        } else {
            _searchState.value = _searchState.value.copy(
                results = emptyList(),
                hasSearched = false,
                errorMessage = null
            )
        }
    }

    fun executeImmediateSearch(query: String) {
        val trimmed = query.trim()
        _searchState.value = _searchState.value.copy(
            query = trimmed,
            filter = _searchState.value.filter.copy(query = trimmed)
        )
        searchQueryFlow.value = trimmed
        executeSearch(_searchState.value.filter, saveToHistory = trimmed.length >= 2)
    }

    private fun executeSearch(filter: AnimeFilterState, saveToHistory: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = _searchState.value.copy(
                isSearching = true,
                errorMessage = null,
                hasSearched = true
            )
            if (saveToHistory && filter.query.isNotBlank()) {
                container.animeRepository.saveRecentSearch(filter.query)
            }
            when (val res = container.animeRepository.searchAnime(filter, 1)) {
                is AppResult.Success -> {
                    _searchState.value = _searchState.value.copy(
                        isSearching = false,
                        results = res.data,
                        errorMessage = null
                    )
                }
                is AppResult.Error -> {
                    _searchState.value = _searchState.value.copy(
                        isSearching = false,
                        errorMessage = res.message
                    )
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch { container.animeRepository.deleteRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { container.animeRepository.clearRecentSearches() }
    }

    fun loadAnimeDetails(anilistId: Int, forceRefresh: Boolean = false) {
        val existing = _animeDetailsMap.value[anilistId]
        if (!forceRefresh && existing is AppResult.Success) return

        viewModelScope.launch {
            _animeDetailsMap.value = _animeDetailsMap.value + (anilistId to AppResult.Loading)
            val result = container.animeRepository.getAnimeDetails(anilistId)
            _animeDetailsMap.value = _animeDetailsMap.value + (anilistId to result)
        }
    }

    fun observeEpisodeProgress(anilistId: Int) =
        container.historyRepository.observeEpisodeProgress(anilistId)

    fun observeAnimeDownloads(anilistId: Int) =
        container.offlineDownloadRepository.observeDownloadsForAnime(anilistId)

    suspend fun inspectEpisodeForDownload(
        anime: Anime,
        episode: Int,
        audioType: String
    ): AppResult<HlsPlaylistInspection> {
        return container.offlineDownloadRepository.inspectEpisodeForDownload(
            anime = anime,
            episode = episode,
            audioType = audioType
        )
    }

    suspend fun startHlsDownload(
        inspection: HlsPlaylistInspection,
        selectedVariant: HlsQualityVariant
    ): AppResult<OfflineDownloadItem> {
        return container.offlineDownloadRepository.startHlsDownload(
            inspection = inspection,
            selectedVariant = selectedVariant
        )
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            container.offlineDownloadRepository.pauseDownload(downloadId)
        }
    }

    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            container.offlineDownloadRepository.resumeDownload(downloadId)
        }
    }

    fun cancelOrDeleteDownload(downloadId: String) {
        viewModelScope.launch {
            container.offlineDownloadRepository.cancelOrDeleteDownload(downloadId)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            container.offlineDownloadRepository.clearAllDownloads()
        }
    }

    fun observePersonalRating(anilistId: Int) =
        container.animeRepository.observePersonalRating(anilistId)

    fun savePersonalRating(anilistId: Int, stars: Float) {
        viewModelScope.launch {
            container.animeRepository.savePersonalRating(anilistId, stars)
        }
    }

    fun toggleWatchlist(anime: Anime) {
        viewModelScope.launch {
            container.watchlistRepository.toggleWatchlist(anime)
        }
    }

    fun updateWatchlistStatus(anime: Anime, status: WatchStatus) {
        viewModelScope.launch {
            container.watchlistRepository.addOrUpdateWatchlist(anime, status)
        }
    }

    fun removeFromWatchlist(anilistId: Int) {
        viewModelScope.launch {
            container.watchlistRepository.removeFromWatchlist(anilistId)
        }
    }

    fun removeHistoryItem(anilistId: Int, episode: Int) {
        viewModelScope.launch {
            container.historyRepository.removeHistoryItem(anilistId, episode)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            container.historyRepository.clearAllHistory()
        }
    }

    fun loadScheduleForDayOffset(dayOffset: Int) {
        _selectedDayOffset.value = dayOffset
        viewModelScope.launch {
            _scheduleState.value = AppResult.Loading
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startEpochSec = cal.timeInMillis / 1000L

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val endEpochSec = cal.timeInMillis / 1000L

            _scheduleState.value = container.scheduleRepository.getWeeklyScheduleForDay(
                dayStartEpochSec = startEpochSec,
                dayEndEpochSec = endEpochSec
            )
        }
    }

    fun toggleScheduleReminder(item: AiringScheduleItem) {
        container.notificationScheduler.toggleEpisodeReminder(item)
    }

    // Onboarding & Preferences actions
    fun completeOnboarding(interests: Set<String>) {
        viewModelScope.launch {
            container.preferencesDataStore.completeOnboarding(interests)
            refreshHome(interests)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { container.preferencesDataStore.setDarkTheme(enabled) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch { container.preferencesDataStore.setAppLanguage(language) }
    }

    fun setDefaultSubtitleLang(lang: String) {
        viewModelScope.launch { container.preferencesDataStore.setDefaultSubtitleLang(lang) }
    }

    fun setDefaultAudioType(audioType: String) {
        viewModelScope.launch { container.preferencesDataStore.setDefaultAudioType(audioType) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { container.preferencesDataStore.setDefaultPlaybackSpeed(speed) }
    }

    fun setDefaultVideoQuality(quality: String) {
        viewModelScope.launch { container.preferencesDataStore.setDefaultVideoQuality(quality) }
    }

    fun setDefaultDisplayMode(mode: com.example.domain.model.VideoDisplayMode) {
        viewModelScope.launch { container.preferencesDataStore.setDefaultDisplayMode(mode) }
    }

    fun setAutoPlayNextEpisode(enabled: Boolean) {
        viewModelScope.launch { container.preferencesDataStore.setAutoPlayNextEpisode(enabled) }
    }

    fun setRememberPlaybackPosition(enabled: Boolean) {
        viewModelScope.launch { container.preferencesDataStore.setRememberPlaybackPosition(enabled) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch {
            container.preferencesDataStore.setWifiOnlyDownloads(enabled)
            container.media3DownloadStack.setWifiOnlyRequirements(enabled)
        }
    }

    fun retryConnectionCheck(): Boolean {
        val online = container.networkConnectivityObserver.retryConnectionCheck()
        if (online && _homeState.value.dashboard.trendingAnime.isEmpty()) {
            refreshHome(preferences.value.selectedInterests)
            loadTopHits()
            loadNewReleases()
            loadScheduleForDayOffset(_selectedDayOffset.value)
        }
        return online
    }

    fun acknowledgeReconnected() {
        container.networkConnectivityObserver.acknowledgeReconnected()
    }

    fun getDownloadsStorageSummary(): Pair<Long, Long> {
        val usedBytes = container.media3DownloadStack.calculateDownloadedBytesOnDisk()
        val freeBytes = container.media3DownloadStack.calculateAvailableFreeBytesOnDisk()
        return usedBytes to freeBytes
    }

    fun setSubtitleAppearance(fontSizeSp: Float, colorHex: String, bgOpacity: Float) {
        viewModelScope.launch {
            container.preferencesDataStore.setSubtitleAppearance(fontSizeSp, colorHex, bgOpacity)
        }
    }

    fun updateNotificationPrefs(
        general: Boolean? = null,
        airing: Boolean? = null,
        newEpisode: Boolean? = null,
        appUpdates: Boolean? = null
    ) {
        viewModelScope.launch {
            container.preferencesDataStore.setNotificationPreferences(
                general = general,
                airing = airing,
                newEpisode = newEpisode,
                appUpdates = appUpdates
            )
        }
    }

    fun calculateCacheSizeMb(context: Context): String {
        val bytes = getDirSize(context.cacheDir)
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }

    fun clearAppCache(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            container.dao.clearMetadataCache()
            container.dao.clearArtworkCache()
            runCatching {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            onComplete()
        }
    }

    fun clearAllLocalData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            container.dao.clearWatchlist()
            container.dao.clearFavorites()
            container.dao.clearAllWatchHistory()
            container.dao.clearAllWatchProgress()
            container.dao.clearRecentSearches()
            container.dao.clearPersonalRatings()
            onComplete()
        }
    }

    suspend fun exportLocalDataJson(): String = container.backupManager.exportJson()

    suspend fun importLocalDataJson(rawJson: String): Boolean = container.backupManager.importJson(rawJson)

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var result = 0L
        dir.listFiles()?.forEach { file ->
            result += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return result
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(container) as T
                }
            }
    }
}
