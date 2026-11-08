package com.example.domain.usecase

import com.example.core.common.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.AnimeFilterState
import com.example.domain.model.StreamResolution
import com.example.domain.model.WatchStatus
import com.example.domain.repository.AniScrollRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.ScheduleRepository
import com.example.domain.repository.WatchlistRepository

class GetHomeDashboardUseCase(private val animeRepository: AnimeRepository) {
    suspend operator fun invoke(preferredGenres: Set<String>) =
        animeRepository.getHomeDashboard(preferredGenres)
}

class SearchAnimeUseCase(private val animeRepository: AnimeRepository) {
    suspend operator fun invoke(filter: AnimeFilterState, page: Int = 1): AppResult<List<Anime>> =
        animeRepository.searchAnime(filter, page)
}

class GetAnimeDetailsUseCase(private val animeRepository: AnimeRepository) {
    suspend operator fun invoke(anilistId: Int): AppResult<Anime> =
        animeRepository.getAnimeDetails(anilistId)
}

class ResolveStreamUseCase(private val aniScrollRepository: AniScrollRepository) {
    suspend operator fun invoke(
        anilistId: Int,
        episode: Int,
        audioType: String
    ): AppResult<StreamResolution> =
        aniScrollRepository.resolveEpisodeStream(anilistId, episode, audioType)
}

class ToggleWatchlistUseCase(private val watchlistRepository: WatchlistRepository) {
    suspend operator fun invoke(anime: Anime) =
        watchlistRepository.toggleWatchlist(anime)

    suspend fun setStatus(anime: Anime, status: WatchStatus) =
        watchlistRepository.addOrUpdateWatchlist(anime, status)
}

class SavePlaybackProgressUseCase(private val historyRepository: HistoryRepository) {
    suspend operator fun invoke(
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
    ) = historyRepository.savePlaybackProgress(
        anime = anime,
        anilistId = anilistId,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
        coverUrl = coverUrl,
        episode = episode,
        totalEpisodes = totalEpisodes,
        audioType = audioType,
        positionMs = positionMs,
        durationMs = durationMs
    )
}

class GetWeeklyScheduleUseCase(private val scheduleRepository: ScheduleRepository) {
    suspend operator fun invoke(dayStartEpochSec: Long, dayEndEpochSec: Long) =
        scheduleRepository.getWeeklyScheduleForDay(dayStartEpochSec, dayEndEpochSec)
}
