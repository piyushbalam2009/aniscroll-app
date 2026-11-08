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
    val hlsProxyUrl: String? = null,
    val fallbackDirectM3u8Url: String? = null,
    val refererUrl: String? = null,
    val playbackMode: String = "m3u8",
    val iframeOnly: Boolean = false,
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
data class HlsQualityVariant(
    val qualityLabel: String, // "360p", "480p", "720p", "1080p"
    val height: Int,          // 360, 480, 720, 1080
    val width: Int = 0,
    val bitrateBps: Int = 0,
    val codecs: String? = null,
    val periodIndex: Int = 0,
    val rendererIndex: Int = 0,
    val trackGroupIndex: Int = 0,
    val trackIndex: Int = 0,
    val isAvailable: Boolean = true
) {
    val formattedBandwidth: String
        get() = if (bitrateBps > 0) {
            val mbps = bitrateBps / 1_000_000.0
            if (mbps >= 1.0) String.format(Locale.US, "%.1f Mbps", mbps)
            else String.format(Locale.US, "%d kbps", bitrateBps / 1000)
        } else {
            when (height) {
                1080 -> "Full HD"
                720 -> "HD"
                480 -> "SD"
                else -> "Data Saver"
            }
        }
}

@Serializable
data class HlsPlaylistInspection(
    val anilistId: Int,
    val episode: Int,
    val audioType: String,
    val animeTitle: String,
    val posterUrl: String,
    val coverUrl: String? = null,
    val totalEpisodes: Int,
    val hlsProxyUrl: String,
    val refererUrl: String? = null,
    val isMasterPlaylist: Boolean,
    val variants: List<HlsQualityVariant>,
    val subtitles: List<SubtitleTrack>
)

@Serializable
enum class OfflineDownloadState(val displayLabel: String) {
    QUEUED("Queued"),
    DOWNLOADING("Downloading"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REMOVING("Removing")
}

@Serializable
data class OfflineDownloadItem(
    val downloadId: String,
    val anilistId: Int,
    val episode: Int,
    val totalEpisodes: Int,
    val audioType: String,
    val animeTitle: String,
    val posterUrl: String,
    val coverUrl: String? = null,
    val qualityLabel: String,
    val videoHeight: Int,
    val hlsProxyUrl: String,
    val refererUrl: String? = null,
    val state: OfflineDownloadState,
    val progressPercent: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val contentLengthBytes: Long = 0L,
    val failureReasonText: String? = null,
    val localSubtitleTracks: List<SubtitleTrack> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val formattedBytesDownloaded: String
        get() {
            val mb = bytesDownloaded.toDouble() / (1024.0 * 1024.0)
            return if (contentLengthBytes > 0L) {
                val totalMb = contentLengthBytes.toDouble() / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB / %.1f MB", mb, totalMb)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

    /**
     * An episode is playable offline ONLY when the download is verified COMPLETED.
     * Queued, downloading, paused, or failed downloads are never marked playable offline.
     */
    val isPlayableOffline: Boolean
        get() = state == OfflineDownloadState.COMPLETED
}

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
enum class VideoDisplayMode(
    val displayName: String,
    val description: String
) {
    FIT(
        displayName = "Fit Screen",
        description = "Show the complete video, preserve aspect ratio, allow black bars."
    ),
    FILL(
        displayName = "Zoom to Fill",
        description = "Fill the entire available landscape screen, preserve aspect ratio, crop excess content."
    ),
    STRETCH(
        displayName = "Stretch",
        description = "Fill the entire screen, allow aspect ratio distortion."
    ),
    ORIGINAL(
        displayName = "Original",
        description = "Preserve original video aspect ratio."
    );

    companion object {
        fun fromNameOrDefault(raw: String?): VideoDisplayMode {
            if (raw.isNullOrBlank()) return FIT
            if (raw.equals("Fill Screen", ignoreCase = true) ||
                raw.equals("Zoom to Fill", ignoreCase = true) ||
                raw.equals("ZOOM", ignoreCase = true)
            ) {
                return FILL
            }
            return entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.displayName.equals(raw, ignoreCase = true)
            } ?: FIT
        }
    }
}

@Serializable
data class UserPreferencesState(
    val darkTheme: Boolean = true,
    val appLanguage: String = "English (US)",
    val defaultSubtitleLang: String = "English",
    val defaultAudioType: String = "sub",
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultVideoQuality: String = "Auto (1080p)",
    val defaultDisplayMode: VideoDisplayMode = VideoDisplayMode.FIT,
    val autoPlayNextEpisode: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
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

