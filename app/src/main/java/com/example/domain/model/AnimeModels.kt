package com.example.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Anime(
    val anilistId: Int,
    val kitsuId: String? = null,
    val englishTitle: String? = null,
    val romajiTitle: String = "",
    val nativeTitle: String? = null,
    val description: String = "",
    val posterUrl: String = "",
    val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val score: Double? = null, // 0..10 scale e.g. 9.8
    val popularity: Int = 0,
    val releaseYear: Int? = null,
    val season: String? = null,
    val format: String? = null, // TV, MOVIE, OVA, ONA, SPECIAL
    val status: String? = null, // RELEASING, FINISHED, NOT_YET_RELEASED
    val totalEpisodes: Int? = null,
    val nextAiringEpisode: Int? = null,
    val nextAiringTimeEpoch: Long? = null,
    val countryOfOrigin: String = "JP",
    val isAdult: Boolean = false,
    val studios: List<String> = emptyList(),
    val characters: List<AnimeCharacter> = emptyList(),
    val recommendations: List<Anime> = emptyList(),
    val relations: List<Anime> = emptyList(),
    val streamingEpisodes: List<EpisodeMeta> = emptyList(),
    val airingSchedule: List<AiringScheduleItem> = emptyList()
) {
    val displayTitle: String
        get() = englishTitle?.takeIf { it.isNotBlank() }
            ?: romajiTitle.takeIf { it.isNotBlank() }
            ?: nativeTitle
            ?: "Unknown Anime"

    val formattedScore: String
        get() = score?.let {
            val normalized = if (it > 10.0) it / 10.0 else it
            String.format(Locale.US, "%.1f", normalized)
        } ?: "N/A"

    val regionDisplay: String
        get() = when (countryOfOrigin.uppercase()) {
            "JP" -> "Japan"
            "CN" -> "China"
            "KR" -> "Korea"
            else -> countryOfOrigin.ifBlank { "Japan" }
        }

    val isMovie: Boolean
        get() = format.equals("MOVIE", ignoreCase = true)

    val availableEpisodeCount: Int
        get() = when {
            nextAiringEpisode != null && nextAiringEpisode > 1 -> nextAiringEpisode - 1
            totalEpisodes != null && totalEpisodes > 0 -> totalEpisodes
            streamingEpisodes.isNotEmpty() -> streamingEpisodes.size
            isMovie -> 1
            else -> 12
        }
}

@Serializable
data class AnimeCharacter(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String?
)

@Serializable
data class EpisodeMeta(
    val number: Int,
    val title: String,
    val thumbnailUrl: String? = null,
    val durationMinutes: Int = 24
)

@Serializable
data class AiringScheduleItem(
    val id: Int,
    val anilistId: Int,
    val episode: Int,
    val airingAtEpoch: Long,
    val timeUntilAiring: Long,
    val anime: Anime? = null
)

@Serializable
data class StreamResolution(
    val anilistId: Int,
    val episode: Int,
    val audioType: String, // "sub" or "dub"
    val title: String,
    val server: String,
    val availableServers: List<String>,
    val hlsUrl: String,
    val fallbackDirectM3u8Url: String?,
    val refererUrl: String?,
    val subtitles: List<SubtitleTrack>,
    val introStartSec: Int? = null,
    val introEndSec: Int? = null,
    val outroStartSec: Int? = null,
    val outroEndSec: Int? = null
)

@Serializable
data class SubtitleTrack(
    val url: String,
    val lang: String,
    val isDefault: Boolean = false
)

@Serializable
enum class WatchStatus(val displayName: String) {
    WATCHING("Watching"),
    PLAN_TO_WATCH("Plan to Watch"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped"),
    FAVORITES("Favorites")
}

@Serializable
data class WatchlistItem(
    val anilistId: Int,
    val anime: Anime,
    val status: WatchStatus,
    val isFavorite: Boolean,
    val addedAtMs: Long,
    val watchedEpisodes: Int = 0
)

@Serializable
data class WatchHistoryItem(
    val id: Long = 0,
    val anilistId: Int,
    val animeTitle: String,
    val posterUrl: String,
    val coverUrl: String?,
    val episode: Int,
    val totalEpisodes: Int,
    val audioType: String,
    val positionMs: Long,
    val durationMs: Long,
    val isCompleted: Boolean,
    val lastWatchedAtMs: Long
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

@Serializable
data class AnimeFilterState(
    val query: String = "",
    val sort: String = "POPULARITY_DESC", // POPULARITY_DESC, SCORE_DESC, START_DATE_DESC, UPDATED_AT_DESC
    val format: String? = null, // TV, MOVIE, OVA, ONA, SPECIAL
    val countryOfOrigin: String? = null, // JP, CN, KR
    val genres: Set<String> = emptySet(),
    val year: Int? = null,
    val season: String? = null, // WINTER, SPRING, SUMMER, FALL
    val status: String? = null // RELEASING, FINISHED, NOT_YET_RELEASED
)

@Serializable
data class LocalNotificationItem(
    val id: Int,
    val anilistId: Int,
    val title: String,
    val episode: Int,
    val posterUrl: String,
    val category: String, // "Update" or "New Release"
    val timestampMs: Long,
    val scheduledEpochSec: Long
)

@Serializable
data class UserPreferencesState(
    val darkTheme: Boolean = true,
    val appLanguage: String = "English (US)",
    val defaultSubtitleLang: String = "English",
    val defaultAudioType: String = "sub",
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultVideoQuality: String = "Auto (1080p)",
    val autoPlayNextEpisode: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val subtitleFontSizeSp: Float = 18f,
    val subtitleColorHex: String = "#FFFFFF",
    val subtitleBackgroundOpacity: Float = 0.65f,
    val selectedInterests: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false,
    val generalNotifications: Boolean = true,
    val airingReminders: Boolean = true,
    val newEpisodeReminders: Boolean = true,
    val appUpdateNotifications: Boolean = true
)
