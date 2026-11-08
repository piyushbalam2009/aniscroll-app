package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.TimeUtils
import com.example.core.common.retryWithBackoff
import com.example.core.database.AniScrollDao
import com.example.core.database.AnimeArtworkEntity
import com.example.core.database.AnimeEntity
import com.example.core.database.FavoriteEntity
import com.example.core.database.PersonalRatingEntity
import com.example.core.database.RecentSearchEntity
import com.example.core.database.ScheduleEntity
import com.example.core.database.WatchHistoryEntity
import com.example.core.database.WatchProgressEntity
import com.example.core.database.WatchlistEntity
import com.example.core.network.NetworkClient
import com.example.data.anilist.AniListQueries
import com.example.data.anilist.AniListService
import com.example.data.anilist.GraphQLRequest
import com.example.data.aniscroll.AniScrollResponseAdapter
import com.example.data.aniscroll.AniScrollService
import com.example.data.kitsu.KitsuMatcher
import com.example.data.kitsu.KitsuService
import com.example.domain.model.AiringScheduleItem
import com.example.domain.model.Anime
import com.example.domain.model.AnimeFilterState
import com.example.domain.model.StreamResolution
import com.example.domain.model.WatchHistoryItem
import com.example.domain.model.WatchStatus
import com.example.domain.model.WatchlistItem
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AniScrollRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.HomeDashboardData
import com.example.domain.repository.KitsuRepository
import com.example.domain.repository.ScheduleRepository
import com.example.domain.repository.WatchlistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.ConcurrentHashMap

object EntityMapper {
    private val json = NetworkClient.json

    fun Anime.toEntity(): AnimeEntity {
        return AnimeEntity(
            anilistId = anilistId,
            kitsuId = kitsuId,
            englishTitle = englishTitle,
            romajiTitle = romajiTitle,
            nativeTitle = nativeTitle,
            description = description,
            posterUrl = posterUrl,
            coverUrl = coverUrl,
            genresJson = json.encodeToString(genres),
            tagsJson = json.encodeToString(tags),
            score = score,
            popularity = popularity,
            releaseYear = releaseYear,
            season = season,
            format = format,
            status = status,
            totalEpisodes = totalEpisodes,
            nextAiringEpisode = nextAiringEpisode,
            nextAiringTimeEpoch = nextAiringTimeEpoch,
            countryOfOrigin = countryOfOrigin,
            studiosJson = json.encodeToString(studios)
        )
    }

    fun AnimeEntity.toDomain(): Anime {
        val genres = runCatching { json.decodeFromString<List<String>>(genresJson) }.getOrDefault(emptyList())
        val tags = runCatching { json.decodeFromString<List<String>>(tagsJson) }.getOrDefault(emptyList())
        val studios = runCatching { json.decodeFromString<List<String>>(studiosJson) }.getOrDefault(emptyList())
        return Anime(
            anilistId = anilistId,
            kitsuId = kitsuId,
            englishTitle = englishTitle,
            romajiTitle = romajiTitle,
            nativeTitle = nativeTitle,
            description = description,
            posterUrl = posterUrl,
            coverUrl = coverUrl,
            genres = genres,
            tags = tags,
            score = score,
            popularity = popularity,
            releaseYear = releaseYear,
            season = season,
            format = format,
            status = status,
            totalEpisodes = totalEpisodes,
            nextAiringEpisode = nextAiringEpisode,
            nextAiringTimeEpoch = nextAiringTimeEpoch,
            countryOfOrigin = countryOfOrigin,
            studios = studios
        )
    }
}

class AniListRepositoryImpl(
    private val service: AniListService,
    private val dao: AniScrollDao
) : AniListRepository {

    private val detailsCache = ConcurrentHashMap<Int, Anime>()

    private suspend fun queryPage(variables: Map<String, JsonElement>): AppResult<List<Anime>> {
        return try {
            val response = retryWithBackoff {
                service.executeGraphQL(
                    GraphQLRequest(
                        query = AniListQueries.PAGE_MEDIA_QUERY,
                        variables = variables
                    )
                )
            }
            val list = response.data?.Page?.media.orEmpty()
                .map { it.toDomain() }
                .filter { !it.isAdult && it.posterUrl.isNotBlank() }

            if (list.isNotEmpty()) {
                dao.insertAnimeList(list.map { with(EntityMapper) { it.toEntity() } })
            }
            AppResult.Success(list)
        } catch (e: Exception) {
            val cached = dao.getCachedPopularAnime(20).map { with(EntityMapper) { it.toDomain() } }
            if (cached.isNotEmpty()) {
                AppResult.Success(cached)
            } else {
                AppResult.Error(e.localizedMessage ?: "Failed to load anime from AniList", e)
            }
        }
    }

    override suspend fun getTrendingAnime(page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "sort" to JsonArray(listOf(JsonPrimitive("TRENDING_DESC"), JsonPrimitive("POPULARITY_DESC")))
            )
        )
    }

    override suspend fun getPopularAnime(page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "sort" to JsonArray(listOf(JsonPrimitive("POPULARITY_DESC")))
            )
        )
    }

    override suspend fun getTopRankedAnime(page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "sort" to JsonArray(listOf(JsonPrimitive("SCORE_DESC"), JsonPrimitive("POPULARITY_DESC")))
            )
        )
    }

    override suspend fun getSeasonalAnime(season: String, year: Int, page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "season" to JsonPrimitive(season),
                "seasonYear" to JsonPrimitive(year),
                "sort" to JsonArray(listOf(JsonPrimitive("POPULARITY_DESC")))
            )
        )
    }

    override suspend fun getUpcomingAnime(page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "status" to JsonPrimitive("NOT_YET_RELEASED"),
                "sort" to JsonArray(listOf(JsonPrimitive("POPULARITY_DESC")))
            )
        )
    }

    override suspend fun getPopularMovies(page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "format" to JsonPrimitive("MOVIE"),
                "sort" to JsonArray(listOf(JsonPrimitive("POPULARITY_DESC"), JsonPrimitive("SCORE_DESC")))
            )
        )
    }

    override suspend fun getNewEpisodeReleases(page: Int, perPage: Int): AppResult<List<Anime>> {
        return queryPage(
            mapOf(
                "page" to JsonPrimitive(page),
                "perPage" to JsonPrimitive(perPage),
                "status" to JsonPrimitive("RELEASING"),
                "sort" to JsonArray(listOf(JsonPrimitive("UPDATED_AT_DESC"), JsonPrimitive("TRENDING_DESC")))
            )
        )
    }

    override suspend fun searchAndFilterAnime(
        filter: AnimeFilterState,
        page: Int,
        perPage: Int
    ): AppResult<List<Anime>> {
        val vars = mutableMapOf<String, JsonElement>(
            "page" to JsonPrimitive(page),
            "perPage" to JsonPrimitive(perPage),
            "sort" to JsonArray(listOf(JsonPrimitive(filter.sort)))
        )
        if (filter.query.isNotBlank()) {
            vars["search"] = JsonPrimitive(filter.query.trim())
        }
        filter.format?.let { vars["format"] = JsonPrimitive(it) }
        filter.status?.let { vars["status"] = JsonPrimitive(it) }
        filter.season?.let { vars["season"] = JsonPrimitive(it) }
        filter.year?.let { vars["seasonYear"] = JsonPrimitive(it) }
        filter.countryOfOrigin?.let { vars["countryOfOrigin"] = JsonPrimitive(it) }
        if (filter.genres.isNotEmpty()) {
            vars["genre_in"] = JsonArray(filter.genres.map { JsonPrimitive(it) })
        }

        return try {
            val response = retryWithBackoff {
                service.executeGraphQL(
                    GraphQLRequest(
                        query = AniListQueries.PAGE_MEDIA_QUERY,
                        variables = vars
                    )
                )
            }
            val list = response.data?.Page?.media.orEmpty()
                .map { it.toDomain() }
                .filter { !it.isAdult && it.posterUrl.isNotBlank() }
            if (list.isNotEmpty()) {
                dao.insertAnimeList(list.map { with(EntityMapper) { it.toEntity() } })
            }
            AppResult.Success(list)
        } catch (e: Exception) {
            AppResult.Error(e.localizedMessage ?: "Search request failed", e)
        }
    }

    override suspend fun getAnimeDetails(anilistId: Int): AppResult<Anime> {
        detailsCache[anilistId]?.let { return AppResult.Success(it) }
        return try {
            val response = retryWithBackoff {
                service.executeGraphQL(
                    GraphQLRequest(
                        query = AniListQueries.MEDIA_DETAILS_QUERY,
                        variables = mapOf("id" to JsonPrimitive(anilistId))
                    )
                )
            }
            val media = response.data?.Media?.toDomain()
            if (media != null) {
                detailsCache[anilistId] = media
                dao.insertAnime(with(EntityMapper) { media.toEntity() })
                AppResult.Success(media)
            } else {
                val cached = dao.getAnimeById(anilistId)?.let { with(EntityMapper) { it.toDomain() } }
                if (cached != null) AppResult.Success(cached)
                else AppResult.Error("Anime details not found")
            }
        } catch (e: Exception) {
            val cached = dao.getAnimeById(anilistId)?.let { with(EntityMapper) { it.toDomain() } }
            if (cached != null) {
                AppResult.Success(cached)
            } else {
                AppResult.Error(e.localizedMessage ?: "Failed to load anime details", e)
            }
        }
    }

    override suspend fun getAiringSchedule(
        startEpochSec: Long,
        endEpochSec: Long,
        page: Int
    ): AppResult<List<AiringScheduleItem>> {
        return try {
            val response = retryWithBackoff {
                service.executeGraphQL(
                    GraphQLRequest(
                        query = AniListQueries.AIRING_SCHEDULE_QUERY,
                        variables = mapOf(
                            "page" to JsonPrimitive(page),
                            "perPage" to JsonPrimitive(45),
                            "airingAtGreater" to JsonPrimitive(startEpochSec.toInt()),
                            "airingAtLesser" to JsonPrimitive(endEpochSec.toInt())
                        )
                    )
                )
            }
            val schedules = response.data?.Page?.airingSchedules.orEmpty()
                .mapNotNull { it.toDomain() }

            if (schedules.isNotEmpty()) {
                val animeEntities = schedules.mapNotNull { it.anime }.map { with(EntityMapper) { it.toEntity() } }
                dao.insertAnimeList(animeEntities)
                dao.insertScheduleItems(
                    schedules.map {
                        ScheduleEntity(
                            scheduleId = it.id,
                            anilistId = it.anilistId,
                            episode = it.episode,
                            airingAtEpoch = it.airingAtEpoch,
                            timeUntilAiring = it.timeUntilAiring
                        )
                    }
                )
            }
            AppResult.Success(schedules)
        } catch (e: Exception) {
            val cachedSchedules = dao.getScheduleBetween(startEpochSec, endEpochSec)
            if (cachedSchedules.isNotEmpty()) {
                val animeMap = dao.getAnimeByIds(cachedSchedules.map { it.anilistId })
                    .associate { it.anilistId to with(EntityMapper) { it.toDomain() } }
                val mapped = cachedSchedules.map {
                    AiringScheduleItem(
                        id = it.scheduleId,
                        anilistId = it.anilistId,
                        episode = it.episode,
                        airingAtEpoch = it.airingAtEpoch,
                        timeUntilAiring = it.airingAtEpoch - (System.currentTimeMillis() / 1000L),
                        anime = animeMap[it.anilistId]
                    )
                }
                AppResult.Success(mapped)
            } else {
                AppResult.Error(e.localizedMessage ?: "Unable to load airing schedule", e)
            }
        }
    }
}

class KitsuRepositoryImpl(
    private val kitsuService: KitsuService,
    private val dao: AniScrollDao
) : KitsuRepository {

    private val mutex = Mutex()

    override suspend fun enrichArtworkIfNeeded(anime: Anime): Anime {
        // 1. Check local Room mapping cache first
        val cachedArtwork = dao.getArtwork(anime.anilistId)
        if (cachedArtwork != null) {
            return anime.copy(
                kitsuId = cachedArtwork.kitsuId,
                posterUrl = cachedArtwork.posterUrl?.takeIf { it.isNotBlank() } ?: anime.posterUrl,
                coverUrl = cachedArtwork.coverUrl?.takeIf { it.isNotBlank() } ?: anime.coverUrl
            )
        }

        // 2. Search Kitsu and verify match
        return try {
            val searchTitle = anime.romajiTitle.ifBlank { anime.displayTitle }
            val response = mutex.withLock {
                kitsuService.searchAnime(query = searchTitle, limit = 5)
            }
            val verified = KitsuMatcher.findVerifiedMatch(
                romajiTitle = anime.romajiTitle,
                englishTitle = anime.englishTitle,
                nativeTitle = anime.nativeTitle,
                releaseYear = anime.releaseYear,
                candidates = response.data.orEmpty()
            )
            if (verified != null) {
                val kitsuPoster = verified.attributes?.posterImage?.bestPosterUrl()
                val kitsuCover = verified.attributes?.coverImage?.bestCoverUrl()
                val finalPoster = kitsuPoster ?: anime.posterUrl
                val finalCover = kitsuCover ?: anime.coverUrl ?: finalPoster

                dao.insertArtwork(
                    AnimeArtworkEntity(
                        anilistId = anime.anilistId,
                        kitsuId = verified.id,
                        posterUrl = finalPoster,
                        coverUrl = finalCover
                    )
                )
                anime.copy(
                    kitsuId = verified.id,
                    posterUrl = finalPoster,
                    coverUrl = finalCover
                )
            } else {
                // Save fallback mapping so we don't repeatedly query Kitsu
                dao.insertArtwork(
                    AnimeArtworkEntity(
                        anilistId = anime.anilistId,
                        kitsuId = null,
                        posterUrl = anime.posterUrl,
                        coverUrl = anime.coverUrl
                    )
                )
                anime
            }
        } catch (_: Exception) {
            // 7. Use AniList artwork if Kitsu matching fails
            anime
        }
    }

    override suspend fun enrichAnimeListArtwork(list: List<Anime>): List<Anime> {
        if (list.isEmpty()) return list
        // Enrich top items with Kitsu artwork while respecting rate limits
        return list.mapIndexed { index, anime ->
            if (index < 6) {
                enrichArtworkIfNeeded(anime)
            } else {
                val cached = dao.getArtwork(anime.anilistId)
                if (cached != null) {
                    anime.copy(
                        kitsuId = cached.kitsuId,
                        posterUrl = cached.posterUrl ?: anime.posterUrl,
                        coverUrl = cached.coverUrl ?: anime.coverUrl
                    )
                } else {
                    anime
                }
            }
        }
    }
}

class AniScrollRepositoryImpl(
    private val service: AniScrollService
) : AniScrollRepository {

    override suspend fun resolveEpisodeStream(
        anilistId: Int,
        episode: Int,
        audioType: String
    ): AppResult<StreamResolution> {
        val normalizedType = if (audioType.equals("dub", ignoreCase = true)) "dub" else "sub"
        return try {
            val rawResponse = retryWithBackoff(maxRetries = 2) {
                service.getWatchStream(
                    anilistId = anilistId,
                    episode = episode,
                    audioType = normalizedType
                )
            }
            val resolution = AniScrollResponseAdapter.parse(
                root = rawResponse,
                requestedAnilistId = anilistId,
                requestedEpisode = episode,
                requestedAudioType = normalizedType
            )
            AppResult.Success(resolution)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("404") == true ->
                    "No ${normalizedType.uppercase()} stream available for Episode $episode on AniScroll."
                else -> e.localizedMessage ?: "Failed to resolve stream from api.aniscroll.in"
            }
            AppResult.Error(msg, e)
        }
    }
}

class AnimeRepositoryImpl(
    private val aniListRepository: AniListRepository,
    private val kitsuRepository: KitsuRepository,
    private val dao: AniScrollDao
) : AnimeRepository {

    override suspend fun getHomeDashboard(preferredGenres: Set<String>): HomeDashboardData = coroutineScope {
        val currentSeason = TimeUtils.getCurrentAnimeSeason()
        val currentYear = TimeUtils.getCurrentYear()

        val trendingDeferred = async { aniListRepository.getTrendingAnime(1, 15) }
        val topHitsDeferred = async { aniListRepository.getTopRankedAnime(1, 15) }
        val newReleasesDeferred = async { aniListRepository.getNewEpisodeReleases(1, 16) }
        val seasonalDeferred = async { aniListRepository.getSeasonalAnime(currentSeason, currentYear, 1, 15) }
        val upcomingDeferred = async { aniListRepository.getUpcomingAnime(1, 12) }
        val moviesDeferred = async { aniListRepository.getPopularMovies(1, 12) }
        val recommendedDeferred = async {
            if (preferredGenres.isNotEmpty()) {
                aniListRepository.searchAndFilterAnime(
                    AnimeFilterState(genres = preferredGenres.take(3).toSet(), sort = "POPULARITY_DESC"),
                    page = 1,
                    perPage = 15
                )
            } else {
                aniListRepository.getPopularAnime(1, 15)
            }
        }

        val trending = (trendingDeferred.await() as? AppResult.Success)?.data.orEmpty()
        val topHits = (topHitsDeferred.await() as? AppResult.Success)?.data.orEmpty()
        val newReleases = (newReleasesDeferred.await() as? AppResult.Success)?.data.orEmpty()
        val seasonal = (seasonalDeferred.await() as? AppResult.Success)?.data.orEmpty()
        val upcoming = (upcomingDeferred.await() as? AppResult.Success)?.data.orEmpty()
        val movies = (moviesDeferred.await() as? AppResult.Success)?.data.orEmpty()
        val recommended = (recommendedDeferred.await() as? AppResult.Success)?.data.orEmpty()

        val featuredCandidate = trending.firstOrNull { !it.coverUrl.isNullOrBlank() }
            ?: topHits.firstOrNull()
        val enrichedFeatured = featuredCandidate?.let { kitsuRepository.enrichArtworkIfNeeded(it) }

        val errorMsg = if (trending.isEmpty() && topHits.isEmpty() && newReleases.isEmpty()) {
            "Unable to reach AniList API. Showing cached content where available."
        } else null

        HomeDashboardData(
            featuredAnime = enrichedFeatured,
            topHits = topHits,
            newEpisodeReleases = newReleases,
            trendingAnime = trending,
            seasonalAnime = seasonal,
            upcomingAnime = upcoming,
            recommendedAnime = recommended,
            popularMovies = movies,
            errorMessage = errorMsg
        )
    }

    override suspend fun getTopHits(page: Int): AppResult<List<Anime>> =
        aniListRepository.getTopRankedAnime(page, 25)

    override suspend fun getNewReleases(page: Int): AppResult<List<Anime>> =
        aniListRepository.getNewEpisodeReleases(page, 24)

    override suspend fun searchAnime(filter: AnimeFilterState, page: Int): AppResult<List<Anime>> =
        aniListRepository.searchAndFilterAnime(filter, page, 24)

    override suspend fun getAnimeDetails(anilistId: Int): AppResult<Anime> {
        return when (val res = aniListRepository.getAnimeDetails(anilistId)) {
            is AppResult.Success -> {
                val enriched = kitsuRepository.enrichArtworkIfNeeded(res.data)
                AppResult.Success(enriched)
            }
            else -> res
        }
    }

    override fun observeRecentSearches(): Flow<List<String>> =
        dao.observeRecentSearches().map { list -> list.map { it.query } }

    override suspend fun saveRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length >= 2) {
            dao.insertRecentSearch(RecentSearchEntity(query = clean))
        }
    }

    override suspend fun deleteRecentSearch(query: String) {
        dao.deleteRecentSearch(query)
    }

    override suspend fun clearRecentSearches() {
        dao.clearRecentSearches()
    }

    override fun observePersonalRating(anilistId: Int): Flow<Float?> =
        dao.observePersonalRating(anilistId).map { it?.ratingStars }

    override suspend fun savePersonalRating(anilistId: Int, stars: Float) {
        dao.insertPersonalRating(PersonalRatingEntity(anilistId = anilistId, ratingStars = stars))
    }
}

class WatchlistRepositoryImpl(
    private val dao: AniScrollDao
) : WatchlistRepository {

    override fun observeWatchlist(): Flow<List<WatchlistItem>> {
        return combine(
            dao.observeAllWatchlist(),
            dao.observeAllFavorites()
        ) { watchlistEntities, favoriteEntities ->
            val favSet = favoriteEntities.map { it.anilistId }.toSet()
            val allIds = (watchlistEntities.map { it.anilistId } + favSet).distinct()
            if (allIds.isEmpty()) return@combine emptyList()

            val animeMap = dao.getAnimeByIds(allIds)
                .associate { it.anilistId to with(EntityMapper) { it.toDomain() } }

            val watchMap = watchlistEntities.associateBy { it.anilistId }

            allIds.mapNotNull { id ->
                val anime = animeMap[id] ?: return@mapNotNull null
                val watchEntity = watchMap[id]
                val isFav = id in favSet
                val status = watchEntity?.status?.let { raw ->
                    runCatching { WatchStatus.valueOf(raw) }.getOrNull()
                } ?: if (isFav) WatchStatus.FAVORITES else WatchStatus.PLAN_TO_WATCH

                WatchlistItem(
                    anilistId = id,
                    anime = anime,
                    status = status,
                    isFavorite = isFav,
                    addedAtMs = watchEntity?.addedAtMs ?: System.currentTimeMillis(),
                    watchedEpisodes = watchEntity?.watchedEpisodes ?: 0
                )
            }.sortedByDescending { it.addedAtMs }
        }
    }

    override fun observeWatchlistIds(): Flow<Set<Int>> =
        dao.observeAllWatchlist().map { list -> list.map { it.anilistId }.toSet() }

    override fun observeWatchStatus(anilistId: Int): Flow<WatchStatus?> =
        dao.observeWatchlistById(anilistId).map { entity ->
            entity?.status?.let { runCatching { WatchStatus.valueOf(it) }.getOrNull() }
        }

    override fun observeIsFavorite(anilistId: Int): Flow<Boolean> =
        dao.observeIsFavorite(anilistId)

    override suspend fun addOrUpdateWatchlist(anime: Anime, status: WatchStatus) {
        dao.insertAnime(with(EntityMapper) { anime.toEntity() })
        if (status == WatchStatus.FAVORITES) {
            dao.insertFavorite(FavoriteEntity(anilistId = anime.anilistId))
        }
        val existing = dao.getWatchlistById(anime.anilistId)
        dao.insertWatchlist(
            WatchlistEntity(
                anilistId = anime.anilistId,
                status = status.name,
                watchedEpisodes = existing?.watchedEpisodes ?: 0,
                addedAtMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun toggleWatchlist(anime: Anime) {
        val existing = dao.getWatchlistById(anime.anilistId)
        if (existing != null) {
            dao.deleteFromWatchlist(anime.anilistId)
        } else {
            addOrUpdateWatchlist(anime, WatchStatus.PLAN_TO_WATCH)
        }
    }

    override suspend fun toggleFavorite(anime: Anime) {
        dao.insertAnime(with(EntityMapper) { anime.toEntity() })
        if (dao.isFavorite(anime.anilistId)) {
            dao.deleteFavorite(anime.anilistId)
        } else {
            dao.insertFavorite(FavoriteEntity(anilistId = anime.anilistId))
        }
    }

    override suspend fun removeFromWatchlist(anilistId: Int) {
        dao.deleteFromWatchlist(anilistId)
        dao.deleteFavorite(anilistId)
    }
}

class HistoryRepositoryImpl(
    private val dao: AniScrollDao
) : HistoryRepository {

    override fun observeWatchHistory(): Flow<List<WatchHistoryItem>> =
        dao.observeWatchHistory().map { list -> list.map { it.toDomain() } }

    override fun observeContinueWatching(): Flow<List<WatchHistoryItem>> =
        dao.observeContinueWatching().map { list ->
            list.distinctBy { it.anilistId }.map { it.toDomain() }
        }

    override fun observeEpisodeProgress(anilistId: Int): Flow<Map<Int, WatchHistoryItem>> =
        dao.observeHistoryForAnime(anilistId).map { list ->
            list.associate { it.episode to it.toDomain() }
        }

    override suspend fun getSavedPositionMs(anilistId: Int, episode: Int): Long {
        return dao.getWatchProgress(anilistId, episode)?.positionMs ?: 0L
    }

    override suspend fun savePlaybackProgress(
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
    ) {
        if (durationMs <= 0L && positionMs <= 0L) return
        anime?.let { dao.insertAnime(with(EntityMapper) { it.toEntity() }) }

        // Prevent overwriting a newer saved position with an older 0ms position on teardown
        val existing = dao.getWatchProgress(anilistId, episode)
        val safePosition = if (positionMs <= 1000L && existing != null && existing.positionMs > 5000L && !existing.isCompleted) {
            existing.positionMs
        } else {
            positionMs.coerceAtLeast(0L)
        }
        val safeDuration = if (durationMs > 0L) durationMs else (existing?.durationMs ?: 0L)
        val isCompleted = safeDuration > 0L && (safePosition.toFloat() / safeDuration.toFloat()) >= 0.90f

        val now = System.currentTimeMillis()
        dao.insertWatchProgress(
            WatchProgressEntity(
                anilistId = anilistId,
                episode = episode,
                audioType = audioType,
                positionMs = safePosition,
                durationMs = safeDuration,
                isCompleted = isCompleted,
                updatedAtMs = now
            )
        )
        dao.insertWatchHistory(
            WatchHistoryEntity(
                anilistId = anilistId,
                episode = episode,
                animeTitle = animeTitle,
                posterUrl = posterUrl,
                coverUrl = coverUrl,
                totalEpisodes = totalEpisodes,
                audioType = audioType,
                positionMs = safePosition,
                durationMs = safeDuration,
                isCompleted = isCompleted,
                lastWatchedAtMs = now
            )
        )
    }

    override suspend fun removeHistoryItem(anilistId: Int, episode: Int) {
        dao.deleteHistoryItem(anilistId, episode)
        dao.deleteProgressItem(anilistId, episode)
    }

    override suspend fun clearAllHistory() {
        dao.clearAllWatchHistory()
        dao.clearAllWatchProgress()
    }

    private fun WatchHistoryEntity.toDomain() = WatchHistoryItem(
        id = anilistId.toLong() * 10000L + episode,
        anilistId = anilistId,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
        coverUrl = coverUrl,
        episode = episode,
        totalEpisodes = totalEpisodes,
        audioType = audioType,
        positionMs = positionMs,
        durationMs = durationMs,
        isCompleted = isCompleted,
        lastWatchedAtMs = lastWatchedAtMs
    )
}

class ScheduleRepositoryImpl(
    private val aniListRepository: AniListRepository
) : ScheduleRepository {
    override suspend fun getWeeklyScheduleForDay(
        dayStartEpochSec: Long,
        dayEndEpochSec: Long
    ): AppResult<List<AiringScheduleItem>> {
        return aniListRepository.getAiringSchedule(dayStartEpochSec, dayEndEpochSec, 1)
    }
}

@Serializable
data class LocalBackupPayload(
    val exportedAtMs: Long = System.currentTimeMillis(),
    val watchlist: List<BackupWatchlistEntry> = emptyList(),
    val favorites: List<Int> = emptyList(),
    val history: List<WatchHistoryItem> = emptyList(),
    val ratings: Map<Int, Float> = emptyMap()
)

@Serializable
data class BackupWatchlistEntry(
    val anilistId: Int,
    val status: String,
    val watchedEpisodes: Int,
    val addedAtMs: Long,
    val anime: Anime? = null
)

class LocalBackupManager(private val dao: AniScrollDao) {
    private val json = NetworkClient.json

    suspend fun exportJson(): String {
        val watchlistSnapshot = dao.getAllWatchlistSnapshot()
        val animeMap = if (watchlistSnapshot.isNotEmpty()) {
            dao.getAnimeByIds(watchlistSnapshot.map { it.anilistId })
                .associate { it.anilistId to with(EntityMapper) { it.toDomain() } }
        } else {
            emptyMap()
        }
        val watchlist = watchlistSnapshot.map {
            BackupWatchlistEntry(
                anilistId = it.anilistId,
                status = it.status,
                watchedEpisodes = it.watchedEpisodes,
                addedAtMs = it.addedAtMs,
                anime = animeMap[it.anilistId]
            )
        }
        val favorites = dao.getAllFavoritesSnapshot().map { it.anilistId }
        val history = dao.getAllHistorySnapshot().map {
            WatchHistoryItem(
                anilistId = it.anilistId,
                animeTitle = it.animeTitle,
                posterUrl = it.posterUrl,
                coverUrl = it.coverUrl,
                episode = it.episode,
                totalEpisodes = it.totalEpisodes,
                audioType = it.audioType,
                positionMs = it.positionMs,
                durationMs = it.durationMs,
                isCompleted = it.isCompleted,
                lastWatchedAtMs = it.lastWatchedAtMs
            )
        }
        val ratings = dao.getAllRatingsSnapshot().associate { it.anilistId to it.ratingStars }
        val payload = LocalBackupPayload(
            watchlist = watchlist,
            favorites = favorites,
            history = history,
            ratings = ratings
        )
        return json.encodeToString(payload)
    }

    suspend fun importJson(rawJson: String): Boolean {
        return try {
            val payload = json.decodeFromString<LocalBackupPayload>(rawJson)
            payload.watchlist.forEach {
                it.anime?.let { anime ->
                    dao.insertAnime(with(EntityMapper) { anime.toEntity() })
                }
                dao.insertWatchlist(
                    WatchlistEntity(
                        anilistId = it.anilistId,
                        status = it.status,
                        watchedEpisodes = it.watchedEpisodes,
                        addedAtMs = it.addedAtMs
                    )
                )
            }
            payload.favorites.forEach {
                dao.insertFavorite(FavoriteEntity(anilistId = it))
            }
            payload.history.forEach {
                dao.insertWatchHistory(
                    WatchHistoryEntity(
                        anilistId = it.anilistId,
                        episode = it.episode,
                        animeTitle = it.animeTitle,
                        posterUrl = it.posterUrl,
                        coverUrl = it.coverUrl,
                        totalEpisodes = it.totalEpisodes,
                        audioType = it.audioType,
                        positionMs = it.positionMs,
                        durationMs = it.durationMs,
                        isCompleted = it.isCompleted,
                        lastWatchedAtMs = it.lastWatchedAtMs
                    )
                )
            }
            payload.ratings.forEach { (id, stars) ->
                dao.insertPersonalRating(PersonalRatingEntity(anilistId = id, ratingStars = stars))
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
