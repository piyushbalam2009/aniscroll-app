package com.example.domain.repository

import com.example.core.common.AppResult
import com.example.domain.model.AiringScheduleItem
import com.example.domain.model.Anime
import com.example.domain.model.AnimeFilterState
import com.example.domain.model.StreamResolution
import com.example.domain.model.WatchHistoryItem
import com.example.domain.model.WatchStatus
import com.example.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface AniListRepository {
    suspend fun getTrendingAnime(page: Int = 1, perPage: Int = 20): AppResult<List<Anime>>
    suspend fun getPopularAnime(page: Int = 1, perPage: Int = 20): AppResult<List<Anime>>
    suspend fun getTopRankedAnime(page: Int = 1, perPage: Int = 25): AppResult<List<Anime>>
    suspend fun getSeasonalAnime(season: String, year: Int, page: Int = 1, perPage: Int = 20): AppResult<List<Anime>>
    suspend fun getUpcomingAnime(page: Int = 1, perPage: Int = 20): AppResult<List<Anime>>
    suspend fun getPopularMovies(page: Int = 1, perPage: Int = 20): AppResult<List<Anime>>
    suspend fun getNewEpisodeReleases(page: Int = 1, perPage: Int = 24): AppResult<List<Anime>>
    suspend fun searchAndFilterAnime(filter: AnimeFilterState, page: Int = 1, perPage: Int = 24): AppResult<List<Anime>>
    suspend fun getAnimeDetails(anilistId: Int): AppResult<Anime>
    suspend fun getAiringSchedule(startEpochSec: Long, endEpochSec: Long, page: Int = 1): AppResult<List<AiringScheduleItem>>
}

interface KitsuRepository {
    suspend fun enrichArtworkIfNeeded(anime: Anime): Anime
    suspend fun enrichAnimeListArtwork(list: List<Anime>): List<Anime>
}

interface AniScrollRepository {
    suspend fun resolveEpisodeStream(
        anilistId: Int,
        episode: Int,
        audioType: String
    ): AppResult<StreamResolution>
}

interface AnimeRepository {
    suspend fun getHomeDashboard(preferredGenres: Set<String>): HomeDashboardData
    suspend fun getTopHits(page: Int = 1): AppResult<List<Anime>>
    suspend fun getNewReleases(page: Int = 1): AppResult<List<Anime>>
    suspend fun searchAnime(filter: AnimeFilterState, page: Int = 1): AppResult<List<Anime>>
    suspend fun getAnimeDetails(anilistId: Int): AppResult<Anime>
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun saveRecentSearch(query: String)
    suspend fun deleteRecentSearch(query: String)
    suspend fun clearRecentSearches()
    fun observePersonalRating(anilistId: Int): Flow<Float?>
    suspend fun savePersonalRating(anilistId: Int, stars: Float)
}

data class HomeDashboardData(
    val featuredAnime: Anime? = null,
    val topHits: List<Anime> = emptyList(),
    val newEpisodeReleases: List<Anime> = emptyList(),
    val trendingAnime: List<Anime> = emptyList(),
    val seasonalAnime: List<Anime> = emptyList(),
    val upcomingAnime: List<Anime> = emptyList(),
    val recommendedAnime: List<Anime> = emptyList(),
    val popularMovies: List<Anime> = emptyList(),
    val errorMessage: String? = null
)

interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<WatchlistItem>>
    fun observeWatchlistIds(): Flow<Set<Int>>
    fun observeWatchStatus(anilistId: Int): Flow<WatchStatus?>
    fun observeIsFavorite(anilistId: Int): Flow<Boolean>
    suspend fun addOrUpdateWatchlist(anime: Anime, status: WatchStatus = WatchStatus.PLAN_TO_WATCH)
    suspend fun toggleWatchlist(anime: Anime)
    suspend fun toggleFavorite(anime: Anime)
    suspend fun removeFromWatchlist(anilistId: Int)
}

interface HistoryRepository {
    fun observeWatchHistory(): Flow<List<WatchHistoryItem>>
    fun observeContinueWatching(): Flow<List<WatchHistoryItem>>
    fun observeEpisodeProgress(anilistId: Int): Flow<Map<Int, WatchHistoryItem>>
    suspend fun getSavedPositionMs(anilistId: Int, episode: Int): Long
    suspend fun savePlaybackProgress(
        anime: Anime?,
        anilistId: Int,
        animeTitle: String,
        posterUrl: String,
        coverUrl: String?,
        episode: Int,
        totalEpisodes: Int,
        audioType: String,
        positionMs: Long,
        durationMs: Long
    )
    suspend fun removeHistoryItem(anilistId: Int, episode: Int)
    suspend fun clearAllHistory()
}

interface ScheduleRepository {
    suspend fun getWeeklyScheduleForDay(dayStartEpochSec: Long, dayEndEpochSec: Long): AppResult<List<AiringScheduleItem>>
}
