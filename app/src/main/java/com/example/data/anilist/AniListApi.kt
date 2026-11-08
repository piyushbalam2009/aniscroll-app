package com.example.data.anilist

import com.example.core.common.TimeUtils
import com.example.domain.model.AiringScheduleItem
import com.example.domain.model.Anime
import com.example.domain.model.AnimeCharacter
import com.example.domain.model.EpisodeMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AniListService {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST(".")
    suspend fun executeGraphQL(@Body request: GraphQLRequest): GraphQLResponse
}

@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class GraphQLResponse(
    val data: AniListDataContainer? = null
)

@Serializable
data class AniListDataContainer(
    val Page: AniListPageDto? = null,
    val Media: AniListMediaDto? = null,
    val GenreCollection: List<String>? = null
)

@Serializable
data class AniListPageDto(
    val pageInfo: AniListPageInfoDto? = null,
    val media: List<AniListMediaDto>? = null,
    val airingSchedules: List<AniListAiringScheduleDto>? = null
)

@Serializable
data class AniListPageInfoDto(
    val total: Int? = null,
    val currentPage: Int? = null,
    val lastPage: Int? = null,
    val hasNextPage: Boolean? = null
)

@Serializable
data class AniListMediaDto(
    val id: Int,
    val title: AniListTitleDto? = null,
    val description: String? = null,
    val coverImage: AniListCoverImageDto? = null,
    val bannerImage: String? = null,
    val genres: List<String>? = null,
    val tags: List<AniListTagDto>? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val seasonYear: Int? = null,
    val startDate: AniListFuzzyDateDto? = null,
    val season: String? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val countryOfOrigin: String? = null,
    val isAdult: Boolean? = null,
    val nextAiringEpisode: AniListNextAiringDto? = null,
    val studios: AniListStudioConnectionDto? = null,
    val characters: AniListCharacterConnectionDto? = null,
    val streamingEpisodes: List<AniListStreamingEpisodeDto>? = null,
    val recommendations: AniListRecommendationConnectionDto? = null,
    val relations: AniListRelationConnectionDto? = null,
    val airingSchedule: AniListAiringConnectionDto? = null
) {
    fun toDomain(): Anime {
        val rawScore = averageScore ?: meanScore
        val normalizedScore = rawScore?.let { it / 10.0 }
        val resolvedYear = seasonYear ?: startDate?.year
        val cleanDesc = TimeUtils.stripHtml(description)
        val poster = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium ?: ""
        val cover = bannerImage ?: poster

        val parsedEpisodes = streamingEpisodes.orEmpty().mapIndexedNotNull { index, ep ->
            val rawTitle = ep.title ?: "Episode ${index + 1}"
            val epNumRegex = Regex("Episode\\s+(\\d+)", RegexOption.IGNORE_CASE)
            val match = epNumRegex.find(rawTitle)
            val epNum = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: (index + 1)
            EpisodeMeta(
                number = epNum,
                title = rawTitle,
                thumbnailUrl = ep.thumbnail ?: cover,
                durationMinutes = duration ?: 24
            )
        }.sortedBy { it.number }

        val recList = recommendations?.nodes.orEmpty()
            .mapNotNull { it.mediaRecommendation?.toDomain() }
            .filter { !it.isAdult }

        val relList = relations?.edges.orEmpty()
            .filter { it.node?.type == "ANIME" || it.node?.type == null }
            .mapNotNull { it.node?.toDomain() }
            .filter { !it.isAdult }

        val charList = characters?.edges.orEmpty().mapNotNull { edge ->
            val node = edge.node ?: return@mapNotNull null
            AnimeCharacter(
                id = node.id ?: 0,
                name = node.name?.full ?: "Character",
                role = edge.role ?: "MAIN",
                imageUrl = node.image?.large ?: node.image?.medium
            )
        }

        val scheduleList = airingSchedule?.nodes.orEmpty().map { node ->
            AiringScheduleItem(
                id = node.id ?: 0,
                anilistId = id,
                episode = node.episode ?: 1,
                airingAtEpoch = node.airingAt ?: 0L,
                timeUntilAiring = node.timeUntilAiring ?: 0L
            )
        }

        return Anime(
            anilistId = id,
            englishTitle = title?.english,
            romajiTitle = title?.romaji ?: title?.english ?: "Unknown",
            nativeTitle = title?.native,
            description = cleanDesc,
            posterUrl = poster,
            coverUrl = cover,
            genres = genres.orEmpty(),
            tags = tags.orEmpty().mapNotNull { it.name }.take(8),
            score = normalizedScore,
            popularity = popularity ?: 0,
            releaseYear = resolvedYear,
            season = season,
            format = format,
            status = status,
            totalEpisodes = episodes,
            nextAiringEpisode = nextAiringEpisode?.episode,
            nextAiringTimeEpoch = nextAiringEpisode?.airingAt,
            countryOfOrigin = countryOfOrigin ?: "JP",
            isAdult = isAdult ?: false,
            studios = studios?.nodes.orEmpty().mapNotNull { it.name }.distinct(),
            characters = charList,
            recommendations = recList,
            relations = relList,
            streamingEpisodes = parsedEpisodes,
            airingSchedule = scheduleList
        )
    }
}

@Serializable
data class AniListTitleDto(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListCoverImageDto(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null
)

@Serializable
data class AniListTagDto(
    val name: String? = null,
    val rank: Int? = null
)

@Serializable
data class AniListFuzzyDateDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class AniListNextAiringDto(
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null,
    val episode: Int? = null
)

@Serializable
data class AniListStudioConnectionDto(
    val nodes: List<AniListStudioNodeDto>? = null
)

@Serializable
data class AniListStudioNodeDto(
    val name: String? = null
)

@Serializable
data class AniListCharacterConnectionDto(
    val edges: List<AniListCharacterEdgeDto>? = null
)

@Serializable
data class AniListCharacterEdgeDto(
    val role: String? = null,
    val node: AniListCharacterNodeDto? = null
)

@Serializable
data class AniListCharacterNodeDto(
    val id: Int? = null,
    val name: AniListCharacterNameDto? = null,
    val image: AniListCharacterImageDto? = null
)

@Serializable
data class AniListCharacterNameDto(
    val full: String? = null
)

@Serializable
data class AniListCharacterImageDto(
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class AniListStreamingEpisodeDto(
    val title: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val site: String? = null
)

@Serializable
data class AniListRecommendationConnectionDto(
    val nodes: List<AniListRecommendationNodeDto>? = null
)

@Serializable
data class AniListRecommendationNodeDto(
    val mediaRecommendation: AniListMediaDto? = null
)

@Serializable
data class AniListRelationConnectionDto(
    val edges: List<AniListRelationEdgeDto>? = null
)

@Serializable
data class AniListRelationEdgeDto(
    val relationType: String? = null,
    val node: AniListRelationNodeDto? = null
)

@Serializable
data class AniListRelationNodeDto(
    val id: Int,
    val type: String? = null,
    val title: AniListTitleDto? = null,
    val coverImage: AniListCoverImageDto? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val seasonYear: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val genres: List<String>? = null,
    val isAdult: Boolean? = null
) {
    fun toDomain(): Anime {
        val rawScore = averageScore ?: meanScore
        val poster = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium ?: ""
        return Anime(
            anilistId = id,
            englishTitle = title?.english,
            romajiTitle = title?.romaji ?: title?.english ?: "Unknown",
            nativeTitle = title?.native,
            posterUrl = poster,
            coverUrl = bannerImage ?: poster,
            genres = genres.orEmpty(),
            score = rawScore?.let { it / 10.0 },
            releaseYear = seasonYear,
            format = format,
            status = status,
            totalEpisodes = episodes,
            isAdult = isAdult ?: false
        )
    }
}

@Serializable
data class AniListAiringConnectionDto(
    val nodes: List<AniListAiringScheduleDto>? = null
)

@Serializable
data class AniListAiringScheduleDto(
    val id: Int? = null,
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null,
    val episode: Int? = null,
    val mediaId: Int? = null,
    val media: AniListMediaDto? = null
) {
    fun toDomain(): AiringScheduleItem? {
        val animeObj = media?.toDomain()
        if (animeObj?.isAdult == true) return null
        val resolvedMediaId = mediaId ?: animeObj?.anilistId ?: return null
        return AiringScheduleItem(
            id = id ?: (resolvedMediaId * 1000 + (episode ?: 1)),
            anilistId = resolvedMediaId,
            episode = episode ?: 1,
            airingAtEpoch = airingAt ?: 0L,
            timeUntilAiring = timeUntilAiring ?: 0L,
            anime = animeObj
        )
    }
}

object AniListQueries {
    private const val MEDIA_CORE_FIELDS = """
        id
        title {
            romaji
            english
            native
        }
        description(asHtml: false)
        coverImage {
            extraLarge
            large
            medium
        }
        bannerImage
        genres
        tags {
            name
            rank
        }
        averageScore
        meanScore
        popularity
        seasonYear
        startDate {
            year
        }
        season
        format
        status
        episodes
        duration
        countryOfOrigin
        isAdult
        nextAiringEpisode {
            airingAt
            timeUntilAiring
            episode
        }
        studios(isMain: true) {
            nodes {
                name
            }
        }
    """

    const val PAGE_MEDIA_QUERY = """
        query PageMedia(
            ${'$'}page: Int = 1,
            ${'$'}perPage: Int = 20,
            ${'$'}sort: [MediaSort] = [POPULARITY_DESC],
            ${'$'}search: String,
            ${'$'}format: MediaFormat,
            ${'$'}status: MediaStatus,
            ${'$'}season: MediaSeason,
            ${'$'}seasonYear: Int,
            ${'$'}countryOfOrigin: CountryCode,
            ${'$'}genre_in: [String]
        ) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                    total
                    currentPage
                    lastPage
                    hasNextPage
                }
                media(
                    type: ANIME,
                    isAdult: false,
                    sort: ${'$'}sort,
                    search: ${'$'}search,
                    format: ${'$'}format,
                    status: ${'$'}status,
                    season: ${'$'}season,
                    seasonYear: ${'$'}seasonYear,
                    countryOfOrigin: ${'$'}countryOfOrigin,
                    genre_in: ${'$'}genre_in
                ) {
                    $MEDIA_CORE_FIELDS
                }
            }
        }
    """

    const val MEDIA_DETAILS_QUERY = """
        query MediaDetails(${'$'}id: Int!) {
            Media(id: ${'$'}id, type: ANIME) {
                $MEDIA_CORE_FIELDS
                characters(sort: [ROLE, RELEVANCE], perPage: 10) {
                    edges {
                        role
                        node {
                            id
                            name {
                                full
                            }
                            image {
                                large
                                medium
                            }
                        }
                    }
                }
                streamingEpisodes {
                    title
                    thumbnail
                    url
                    site
                }
                recommendations(sort: [RATING_DESC], perPage: 12) {
                    nodes {
                        mediaRecommendation {
                            $MEDIA_CORE_FIELDS
                        }
                    }
                }
                relations {
                    edges {
                        relationType
                        node {
                            id
                            type
                            title {
                                romaji
                                english
                                native
                            }
                            coverImage {
                                extraLarge
                                large
                                medium
                            }
                            bannerImage
                            averageScore
                            meanScore
                            seasonYear
                            format
                            status
                            episodes
                            genres
                            isAdult
                        }
                    }
                }
                airingSchedule(notYetAired: false, perPage: 25) {
                    nodes {
                        id
                        airingAt
                        timeUntilAiring
                        episode
                    }
                }
            }
        }
    """

    const val AIRING_SCHEDULE_QUERY = """
        query AiringSchedule(
            ${'$'}page: Int = 1,
            ${'$'}perPage: Int = 40,
            ${'$'}airingAtGreater: Int,
            ${'$'}airingAtLesser: Int
        ) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                    hasNextPage
                }
                airingSchedules(
                    airingAt_greater: ${'$'}airingAtGreater,
                    airingAt_lesser: ${'$'}airingAtLesser,
                    sort: [TIME]
                ) {
                    id
                    airingAt
                    timeUntilAiring
                    episode
                    mediaId
                    media {
                        $MEDIA_CORE_FIELDS
                    }
                }
            }
        }
    """
}
