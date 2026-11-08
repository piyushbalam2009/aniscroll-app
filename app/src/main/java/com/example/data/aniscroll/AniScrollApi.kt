package com.example.data.aniscroll

import com.example.domain.model.StreamResolution
import com.example.domain.model.SubtitleTrack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.http.GET
import retrofit2.http.Path

interface AniScrollService {
    @GET("api/watch/megaplay/{anilistId}/{episode}/{audioType}")
    suspend fun getWatchStream(
        @Path("anilistId") anilistId: Int,
        @Path("episode") episode: Int,
        @Path("audioType") audioType: String
    ): JsonObject
}

object AniScrollResponseAdapter {
    /**
     * Parses actual AniScroll watch responses from:
     * GET https://api.aniscroll.in/api/watch/megaplay/{anilist_id}/{episode}/{sub/dub}
     *
     * Validated against real responses containing:
     * anilistId, malId, title, episode, type, source, server, availableServers,
     * embedUrl, m3u8, hlsProxyUrl, playbackMode, iframeOnly, subtitles, intro, outro, note.
     * Also safely handles nested `data` or `sources` structures if the provider updates format.
     */
    fun parse(
        root: JsonObject,
        requestedAnilistId: Int,
        requestedEpisode: Int,
        requestedAudioType: String
    ): StreamResolution {
        // Unwrap optional "data" envelope if present
        val payload = (root["data"] as? JsonObject) ?: root

        val anilistId = payload["anilistId"]?.jsonPrimitive?.intOrNull ?: requestedAnilistId
        val episode = payload["episode"]?.jsonPrimitive?.intOrNull ?: requestedEpisode
        val audioType = payload["type"]?.jsonPrimitive?.contentOrNull ?: requestedAudioType
        val title = payload["title"]?.jsonPrimitive?.contentOrNull ?: "Episode $episode"
        val server = payload["server"]?.jsonPrimitive?.contentOrNull ?: "MegaPlay Direct"
        val refererUrl = payload["embedUrl"]?.jsonPrimitive?.contentOrNull
            ?: payload["referer"]?.jsonPrimitive?.contentOrNull

        val availableServers = (payload["availableServers"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.ifEmpty { listOf(server) }
            ?: listOf(server)

        // Direct m3u8 and proxied HLS URL
        val directM3u8 = payload["m3u8"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("https://") }
            ?: extractFromSourcesArray(payload)
        val hlsProxyUrl = payload["hlsProxyUrl"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("https://") }

        // Prefer directM3u8 with Referer header or hlsProxyUrl; keep the other as automatic fallback
        val primaryHls = directM3u8 ?: hlsProxyUrl
            ?: throw IllegalStateException("No valid HTTPS HLS stream URL returned for episode $episode ($audioType).")
        val fallbackHls = if (primaryHls == directM3u8) hlsProxyUrl else directM3u8

        // Extract and filter subtitles (excluding thumbnail sprite preview tracks)
        val rawSubtitles = (payload["subtitles"] as? JsonArray)
            ?: (payload["tracks"] as? JsonArray)
            ?: JsonArray(emptyList())

        val subtitles = rawSubtitles.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val subUrl = obj["url"]?.jsonPrimitive?.contentOrNull
                ?: obj["file"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            if (!subUrl.startsWith("https://")) return@mapNotNull null
            val lang = obj["lang"]?.jsonPrimitive?.contentOrNull
                ?: obj["label"]?.jsonPrimitive?.contentOrNull
                ?: "Subtitle"
            val kind = obj["kind"]?.jsonPrimitive?.contentOrNull ?: "captions"

            // Skip sprite sheet thumbnail VTT tracks
            if (subUrl.contains("thumbnails.vtt", ignoreCase = true) ||
                lang.equals("thumbnails", ignoreCase = true) ||
                kind.equals("thumbnails", ignoreCase = true) ||
                (lang.equals("Unknown", ignoreCase = true) && subUrl.contains("/preview/", ignoreCase = true))
            ) {
                return@mapNotNull null
            }

            val isDefault = obj["default"]?.jsonPrimitive?.booleanOrNull ?: false
            SubtitleTrack(
                url = subUrl,
                lang = lang,
                isDefault = isDefault
            )
        }

        val introObj = payload["intro"] as? JsonObject
        val outroObj = payload["outro"] as? JsonObject

        return StreamResolution(
            anilistId = anilistId,
            episode = episode,
            audioType = audioType,
            title = title,
            server = server,
            availableServers = availableServers,
            hlsUrl = primaryHls,
            fallbackDirectM3u8Url = fallbackHls,
            refererUrl = refererUrl,
            subtitles = subtitles,
            introStartSec = introObj?.get("start")?.jsonPrimitive?.intOrNull,
            introEndSec = introObj?.get("end")?.jsonPrimitive?.intOrNull,
            outroStartSec = outroObj?.get("start")?.jsonPrimitive?.intOrNull,
            outroEndSec = outroObj?.get("end")?.jsonPrimitive?.intOrNull
        )
    }

    private fun extractFromSourcesArray(payload: JsonObject): String? {
        val sources = payload["sources"] as? JsonArray ?: return null
        for (item in sources) {
            val obj = item as? JsonObject ?: continue
            val url = obj["url"]?.jsonPrimitive?.contentOrNull
                ?: obj["file"]?.jsonPrimitive?.contentOrNull
            if (url != null && url.startsWith("https://")) {
                return url
            }
        }
        return null
    }

    private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
        get() = if (isString) content else content.takeIf { it != "null" }
}
