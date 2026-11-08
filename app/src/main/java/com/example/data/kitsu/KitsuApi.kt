package com.example.data.kitsu

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface KitsuService {
    @Headers("Accept: application/vnd.api+json")
    @GET("anime")
    suspend fun searchAnime(
        @Query("filter[text]") query: String,
        @Query("page[limit]") limit: Int = 5
    ): KitsuSearchResponse
}

@Serializable
data class KitsuSearchResponse(
    val data: List<KitsuAnimeResource>? = null
)

@Serializable
data class KitsuAnimeResource(
    val id: String,
    val type: String? = null,
    val attributes: KitsuAnimeAttributes? = null
)

@Serializable
data class KitsuAnimeAttributes(
    val slug: String? = null,
    val canonicalTitle: String? = null,
    val titles: KitsuTitlesDto? = null,
    val startDate: String? = null,
    val subtype: String? = null,
    val posterImage: KitsuImageSetDto? = null,
    val coverImage: KitsuImageSetDto? = null
)

@Serializable
data class KitsuTitlesDto(
    val en: String? = null,
    val en_jp: String? = null,
    val en_us: String? = null,
    val ja_jp: String? = null
)

@Serializable
data class KitsuImageSetDto(
    val original: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val small: String? = null
) {
    fun bestPosterUrl(): String? = large ?: original ?: medium ?: small
    fun bestCoverUrl(): String? = large ?: original ?: medium ?: small
}

object KitsuMatcher {
    private fun normalizeTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun findVerifiedMatch(
        romajiTitle: String,
        englishTitle: String?,
        nativeTitle: String?,
        releaseYear: Int?,
        candidates: List<KitsuAnimeResource>
    ): KitsuAnimeResource? {
        if (candidates.isEmpty()) return null
        val targetTitles = listOfNotNull(
            normalizeTitle(romajiTitle).takeIf { it.isNotBlank() },
            normalizeTitle(englishTitle).takeIf { it.isNotBlank() },
            nativeTitle?.trim()?.takeIf { it.isNotBlank() }
        )
        if (targetTitles.isEmpty()) return null

        var bestCandidate: KitsuAnimeResource? = null
        var bestScore = 0

        for (candidate in candidates) {
            val attrs = candidate.attributes ?: continue
            val candidateTitles = listOfNotNull(
                normalizeTitle(attrs.canonicalTitle),
                normalizeTitle(attrs.titles?.en),
                normalizeTitle(attrs.titles?.en_jp),
                normalizeTitle(attrs.titles?.en_us),
                attrs.titles?.ja_jp?.trim()
            ).filter { it.isNotBlank() }

            var score = 0
            for (target in targetTitles) {
                for (cand in candidateTitles) {
                    if (target == cand) {
                        score = maxOf(score, 100)
                    } else if (target.length >= 4 && (cand.contains(target) || target.contains(cand))) {
                        score = maxOf(score, 75)
                    }
                }
            }

            // Verify year alignment if available
            val candidateYear = attrs.startDate?.take(4)?.toIntOrNull()
            if (releaseYear != null && candidateYear != null) {
                if (kotlin.math.abs(releaseYear - candidateYear) <= 1) {
                    score += 20
                } else {
                    score -= 30
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        // Only accept verified matches (score >= 70)
        return if (bestScore >= 70) bestCandidate else null
    }
}
