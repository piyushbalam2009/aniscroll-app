package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_metadata")
data class AnimeEntity(
    @PrimaryKey val anilistId: Int,
    val kitsuId: String? = null,
    val englishTitle: String? = null,
    val romajiTitle: String,
    val nativeTitle: String? = null,
    val description: String,
    val posterUrl: String,
    val coverUrl: String? = null,
    val genresJson: String,
    val tagsJson: String,
    val score: Double? = null,
    val popularity: Int = 0,
    val releaseYear: Int? = null,
    val season: String? = null,
    val format: String? = null,
    val status: String? = null,
    val totalEpisodes: Int? = null,
    val nextAiringEpisode: Int? = null,
    val nextAiringTimeEpoch: Long? = null,
    val countryOfOrigin: String = "JP",
    val studiosJson: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "anime_artwork")
data class AnimeArtworkEntity(
    @PrimaryKey val anilistId: Int,
    val kitsuId: String?,
    val posterUrl: String?,
    val coverUrl: String?,
    val cachedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_progress", primaryKeys = ["anilistId", "episode"])
data class WatchProgressEntity(
    val anilistId: Int,
    val episode: Int,
    val audioType: String,
    val positionMs: Long,
    val durationMs: Long,
    val isCompleted: Boolean,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history", primaryKeys = ["anilistId", "episode"])
data class WatchHistoryEntity(
    val anilistId: Int,
    val episode: Int,
    val animeTitle: String,
    val posterUrl: String,
    val coverUrl: String?,
    val totalEpisodes: Int,
    val audioType: String,
    val positionMs: Long,
    val durationMs: Long,
    val isCompleted: Boolean,
    val lastWatchedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val anilistId: Int,
    val status: String, // WATCHING, PLAN_TO_WATCH, COMPLETED, ON_HOLD, DROPPED, FAVORITES
    val watchedEpisodes: Int = 0,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val anilistId: Int,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "episodes", primaryKeys = ["anilistId", "episodeNumber"])
data class EpisodeEntity(
    val anilistId: Int,
    val episodeNumber: Int,
    val title: String,
    val thumbnailUrl: String?,
    val durationMinutes: Int = 24
)

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey val scheduleId: Int,
    val anilistId: Int,
    val episode: Int,
    val airingAtEpoch: Long,
    val timeUntilAiring: Long,
    val cachedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "personal_ratings")
data class PersonalRatingEntity(
    @PrimaryKey val anilistId: Int,
    val ratingStars: Float,
    val ratedAtMs: Long = System.currentTimeMillis()
)
