package com.example.playback.download

import android.net.Uri
import com.example.domain.model.HlsQualityVariant
import com.example.domain.model.StreamResolution
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class HlsManifestValidationResult {
    data class Valid(
        val hlsProxyUrl: String,
        val refererUrl: String?,
        val isMasterPlaylist: Boolean,
        val rawManifest: String,
        val variants: List<HlsQualityVariant>
    ) : HlsManifestValidationResult()

    data class Invalid(
        val failureTitle: String,
        val exactFailureReason: String
    ) : HlsManifestValidationResult()
}

/**
 * Validates `hlsProxyUrl` from the AniScroll watch endpoint, checks for DRM or provider
 * restrictions, inspects the `#EXTM3U` master/media playlist, and determines which of
 * `360p`, `480p`, `720p`, and `1080p` are actually available.
 */
object HlsManifestValidator {

    val SUPPORTED_TIERS = listOf(360, 480, 720, 1080)

    private val RESOLUTION_REGEX = Regex("""RESOLUTION=(\d+)x(\d+)""", RegexOption.IGNORE_CASE)
    private val BANDWIDTH_REGEX = Regex("""(?:AVERAGE-)?BANDWIDTH=(\d+)""", RegexOption.IGNORE_CASE)
    private val CODECS_REGEX = Regex("""CODECS="([^"]+)"""", RegexOption.IGNORE_CASE)
    private val KEY_METHOD_REGEX = Regex("""METHOD=([A-Z0-9\-]+)""", RegexOption.IGNORE_CASE)
    private val KEYFORMAT_REGEX = Regex("""KEYFORMAT="([^"]+)"""", RegexOption.IGNORE_CASE)

    fun buildAuthorizedHeaders(refererUrl: String?): Map<String, String> {
        val cleanReferer = refererUrl?.takeIf { it.startsWith("http") } ?: "https://megaplay.buzz/"
        val origin = runCatching {
            val uri = URI(cleanReferer)
            "${uri.scheme}://${uri.authority}"
        }.getOrDefault(cleanReferer.trimEnd('/'))

        return mapOf(
            "Referer" to cleanReferer,
            "Origin" to origin,
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36",
            "Accept" to "application/vnd.apple.mpegurl,application/x-mpegURL,text/plain,*/*"
        )
    }

    /**
     * Resolves a relative child playlist or segment URI against an `hlsProxyUrl`
     * (such as `https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2Fmaster.m3u8`).
     */
    fun resolveAgainstHlsProxyUrl(baseProxyUrl: String, childUri: String): String {
        val trimmedChild = childUri.trim()
        if (trimmedChild.startsWith("https://") || trimmedChild.startsWith("http://")) {
            return trimmedChild
        }
        if (trimmedChild.startsWith("/api/proxy/")) {
            return "https://api.aniscroll.in$trimmedChild"
        }

        // Check if baseProxyUrl is an AniScroll proxy URL with a ?url= parameter
        val embeddedUpstream = extractEmbeddedUpstreamUrl(baseProxyUrl)
        if (embeddedUpstream != null) {
            val resolvedUpstream = runCatching {
                URI(embeddedUpstream).resolve(trimmedChild).toString()
            }.getOrNull()
            if (resolvedUpstream != null && resolvedUpstream.startsWith("http")) {
                val proxyPrefix = baseProxyUrl.substringBefore("?url=")
                val encoded = URLEncoder.encode(resolvedUpstream, "UTF-8")
                return "$proxyPrefix?url=$encoded"
            }
        }

        return runCatching {
            URI(baseProxyUrl).resolve(trimmedChild).toString()
        }.getOrDefault(trimmedChild)
    }

    /**
     * If Media3's `UriUtil.resolve` resolved a relative URI like `seg-1.ts` or `720p/index.m3u8`
     * against `https://api.aniscroll.in/api/proxy/hls?url=...`, the resulting URI becomes
     * `https://api.aniscroll.in/api/proxy/seg-1.ts` (missing `/hls?url=...`).
     * This helper rewrites it back through the proxy using the original upstream base URL.
     */
    fun rewriteMisresolvedProxyUriIfNeeded(
        requestedUri: Uri,
        masterProxyUrl: String?
    ): Uri {
        if (masterProxyUrl.isNullOrBlank()) return requestedUri
        val host = requestedUri.host ?: return requestedUri
        val path = requestedUri.path ?: return requestedUri

        // If it's already a valid /api/proxy/hls?url=... request, leave as-is
        if (host.equals("api.aniscroll.in", ignoreCase = true) &&
            path.startsWith("/api/proxy/") &&
            !path.startsWith("/api/proxy/hls")
        ) {
            val relativeSuffix = path.removePrefix("/api/proxy/")
            val upstreamMaster = extractEmbeddedUpstreamUrl(masterProxyUrl)
            if (upstreamMaster != null && relativeSuffix.isNotBlank()) {
                val resolvedUpstream = runCatching {
                    URI(upstreamMaster).resolve(relativeSuffix).toString()
                }.getOrNull()
                if (resolvedUpstream != null) {
                    val encoded = URLEncoder.encode(resolvedUpstream, "UTF-8")
                    return Uri.parse("https://api.aniscroll.in/api/proxy/hls?url=$encoded")
                }
            }
        }
        return requestedUri
    }

    fun extractEmbeddedUpstreamUrl(proxyUrl: String): String? {
        val queryPart = proxyUrl.substringAfter("?", "")
        if (queryPart.isBlank()) return null
        val urlParam = queryPart.split("&")
            .firstOrNull { it.startsWith("url=", ignoreCase = true) }
            ?.substringAfter("=")
            ?: return null
        return runCatching {
            URLDecoder.decode(urlParam, "UTF-8")
        }.getOrNull()?.takeIf { it.startsWith("http") }
    }

    /**
     * Validates a raw HLS manifest string for `#EXTM3U` compliance, DRM restrictions,
     * and parses available quality variants (`360p`, `480p`, `720p`, `1080p`).
     */
    fun parseAndValidateManifestText(
        hlsProxyUrl: String,
        refererUrl: String?,
        rawManifest: String
    ): HlsManifestValidationResult {
        val cleaned = rawManifest.trim().removePrefix("\uFEFF").trim()
        if (cleaned.isEmpty()) {
            return HlsManifestValidationResult.Invalid(
                failureTitle = "Empty HLS Playlist",
                exactFailureReason = "The AniScroll hlsProxyUrl returned an empty response body instead of an #EXTM3U HLS manifest."
            )
        }

        if (!cleaned.startsWith("#EXTM3U")) {
            val preview = cleaned.lineSequence().firstOrNull()?.take(80).orEmpty()
            val reason = when {
                cleaned.startsWith("<!DOCTYPE", ignoreCase = true) ||
                    cleaned.startsWith("<html", ignoreCase = true) ->
                    "The HLS proxy returned an HTML page instead of an #EXTM3U playlist. The upstream stream URL may have expired or is blocked by Cloudflare/provider protection."
                cleaned.startsWith("{") ->
                    "The HLS proxy returned a JSON error payload ($preview) instead of an #EXTM3U playlist."
                else ->
                    "Unsupported playlist format returned by hlsProxyUrl (missing #EXTM3U header)."
            }
            return HlsManifestValidationResult.Invalid(
                failureTitle = "Unsupported HLS Playlist",
                exactFailureReason = reason
            )
        }

        // Check for DRM / encryption restrictions
        val drmRestriction = detectDrmRestriction(cleaned)
        if (drmRestriction != null) {
            return HlsManifestValidationResult.Invalid(
                failureTitle = "DRM-Protected Stream",
                exactFailureReason = drmRestriction
            )
        }

        val isMasterPlaylist = cleaned.contains("#EXT-X-STREAM-INF")
        val hasMediaSegments = cleaned.contains("#EXTINF")

        if (!isMasterPlaylist && !hasMediaSegments) {
            return HlsManifestValidationResult.Invalid(
                failureTitle = "Invalid HLS Manifest Structure",
                exactFailureReason = "The #EXTM3U playlist contains neither #EXT-X-STREAM-INF variant streams nor #EXTINF media segments."
            )
        }

        val variants = if (isMasterPlaylist) {
            extractMasterPlaylistVariants(cleaned)
        } else {
            inferSinglePlaylistVariants(hlsProxyUrl)
        }

        return HlsManifestValidationResult.Valid(
            hlsProxyUrl = hlsProxyUrl,
            refererUrl = refererUrl,
            isMasterPlaylist = isMasterPlaylist,
            rawManifest = cleaned,
            variants = variants
        )
    }

    /**
     * Checks `#EXT-X-KEY` and `#EXT-X-SESSION-KEY` lines for DRM systems
     * (`SAMPLE-AES`, `SAMPLE-AES-CTR`, `SAMPLE-AES-CENC`, Widevine, FairPlay, PlayReady).
     */
    fun detectDrmRestriction(manifestText: String): String? {
        val lines = manifestText.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("#EXT-X-KEY", ignoreCase = true) ||
                line.startsWith("#EXT-X-SESSION-KEY", ignoreCase = true)
            ) {
                val method = KEY_METHOD_REGEX.find(line)?.groupValues?.getOrNull(1)?.uppercase()
                val keyFormat = KEYFORMAT_REGEX.find(line)?.groupValues?.getOrNull(1)?.lowercase()

                if (method != null && method != "NONE" && method != "AES-128") {
                    return "Stream uses $method DRM encryption. Offline downloading of DRM-protected streams is restricted and cannot be bypassed."
                }
                if (keyFormat != null && keyFormat != "identity") {
                    val drmName = when {
                        keyFormat.contains("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed") -> "Widevine DRM"
                        keyFormat.contains("com.apple.streamingkeydelivery") -> "Apple FairPlay DRM"
                        keyFormat.contains("com.microsoft.playready") -> "Microsoft PlayReady DRM"
                        else -> "DRM ($keyFormat)"
                    }
                    return "Stream is protected by $drmName. Offline downloading of DRM-protected content is not permitted."
                }
            }
        }
        return null
    }

    /**
     * Extracts `#EXT-X-STREAM-INF` variants from a master playlist and maps them
     * onto the 4 supported selectable quality tiers (`360p`, `480p`, `720p`, `1080p`).
     * Only tiers actually present in the master playlist have `isAvailable = true`.
     */
    fun extractMasterPlaylistVariants(masterManifest: String): List<HlsQualityVariant> {
        data class DiscoveredStreamInf(
            val width: Int,
            val height: Int,
            val bandwidth: Int,
            val codecs: String?,
            val streamIndex: Int
        )

        val discovered = mutableListOf<DiscoveredStreamInf>()
        val lines = masterManifest.lines()
        var streamIdx = 0

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF:", ignoreCase = true)) {
                val resMatch = RESOLUTION_REGEX.find(line)
                val bwMatch = BANDWIDTH_REGEX.find(line)
                val codecsMatch = CODECS_REGEX.find(line)

                val width = resMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val height = resMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
                    ?: inferHeightFromUriOrBandwidth(
                        nextUriLine = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }.orEmpty(),
                        bandwidth = bwMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                    )
                val bandwidth = bwMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val codecs = codecsMatch?.groupValues?.getOrNull(1)

                if (height > 0) {
                    discovered.add(
                        DiscoveredStreamInf(
                            width = if (width > 0) width else (height * 16) / 9,
                            height = height,
                            bandwidth = bandwidth,
                            codecs = codecs,
                            streamIndex = streamIdx
                        )
                    )
                }
                streamIdx++
            }
            i++
        }

        return SUPPORTED_TIERS.map { targetHeight ->
            val match = discovered
                .filter { mapHeightToTier(it.height) == targetHeight }
                .maxByOrNull { it.bandwidth }

            if (match != null) {
                HlsQualityVariant(
                    qualityLabel = "${targetHeight}p",
                    height = targetHeight,
                    width = match.width,
                    bitrateBps = match.bandwidth,
                    codecs = match.codecs,
                    trackIndex = match.streamIndex,
                    isAvailable = true
                )
            } else {
                HlsQualityVariant(
                    qualityLabel = "${targetHeight}p",
                    height = targetHeight,
                    width = (targetHeight * 16) / 9,
                    bitrateBps = 0,
                    isAvailable = false
                )
            }
        }
    }

    fun mapHeightToTier(rawHeight: Int): Int? = when (rawHeight) {
        in 300..419 -> 360
        in 420..599 -> 480
        in 600..899 -> 720
        in 900..1200 -> 1080
        else -> null
    }

    private fun inferHeightFromUriOrBandwidth(nextUriLine: String, bandwidth: Int): Int {
        val lower = nextUriLine.lowercase()
        return when {
            lower.contains("1080") -> 1080
            lower.contains("720") -> 720
            lower.contains("480") -> 480
            lower.contains("360") -> 360
            bandwidth >= 3_500_000 -> 1080
            bandwidth >= 1_800_000 -> 720
            bandwidth >= 900_000 -> 480
            bandwidth > 0 -> 360
            else -> 0
        }
    }

    private fun inferSinglePlaylistVariants(hlsProxyUrl: String): List<HlsQualityVariant> {
        val detectedTier = inferHeightFromUriOrBandwidth(hlsProxyUrl, 0)
            .takeIf { it in SUPPORTED_TIERS }
            ?: 720

        return SUPPORTED_TIERS.map { tier ->
            HlsQualityVariant(
                qualityLabel = "${tier}p",
                height = tier,
                width = (tier * 16) / 9,
                isAvailable = tier == detectedTier
            )
        }
    }

    private val URI_ATTRIBUTE_REGEX = Regex("""URI="([^"]+)"""")

    /**
     * Ensures all relative or upstream URIs inside an `#EXTM3U` playlist (including `#EXT-X-MAP`,
     * `#EXT-X-MEDIA`, `#EXT-X-KEY`, and segment/variant lines) are consistently resolved through
     * `baseProxyUrl` so Media3 `DownloadHelper`, `HlsDownloader`, and `SimpleCache` use consistent URIs.
     */
    fun rewriteManifestUrisForProxy(rawManifest: String, baseProxyUrl: String): String {
        return rawManifest.lineSequence().joinToString("\n") { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.isEmpty() -> rawLine
                trimmed.startsWith("#") -> {
                    if (trimmed.contains("URI=\"")) {
                        URI_ATTRIBUTE_REGEX.replace(rawLine) { matchResult ->
                            val originalUri = matchResult.groupValues[1]
                            val rewritten = resolveAgainstHlsProxyUrl(baseProxyUrl, originalUri)
                            "URI=\"$rewritten\""
                        }
                    } else {
                        rawLine
                    }
                }
                else -> resolveAgainstHlsProxyUrl(baseProxyUrl, trimmed)
            }
        }
    }

    /**
     * Executes an HTTP GET to `targetProxyUrl`. If `api.aniscroll.in/api/proxy/hls` returns
     * `HTTP 429` (rate limit) or `502`/`503`/`504`, falls back to fetching the embedded
     * upstream HLS URL directly using the same authorized headers.
     */
    private fun fetchManifestWith429Fallback(
        okHttpClient: OkHttpClient,
        targetProxyUrl: String,
        headers: Map<String, String>
    ): Pair<Int, String?> {
        val primaryReq = Request.Builder().url(targetProxyUrl).get().apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()

        val primaryCode: Int
        val primaryBody: String?
        okHttpClient.newCall(primaryReq).execute().use { resp ->
            primaryCode = resp.code
            primaryBody = if (resp.isSuccessful) resp.body?.string() else null
        }

        if (primaryCode in 200..299 && !primaryBody.isNullOrBlank()) {
            return primaryCode to primaryBody
        }

        // If the AniScroll proxy rate-limited the request (429) or had a gateway timeout (502/503/504),
        // fall back to the embedded upstream URL decoded from targetProxyUrl with authorized headers.
        if (primaryCode == 429 || primaryCode in 502..504) {
            val embeddedUpstream = extractEmbeddedUpstreamUrl(targetProxyUrl)
            if (embeddedUpstream != null) {
                val fallbackReq = Request.Builder().url(embeddedUpstream).get().apply {
                    headers.forEach { (k, v) -> header(k, v) }
                }.build()
                runCatching {
                    okHttpClient.newCall(fallbackReq).execute().use { fallbackResp ->
                        if (fallbackResp.isSuccessful) {
                            val rawText = fallbackResp.body?.string().orEmpty()
                            if (rawText.isNotBlank()) {
                                return fallbackResp.code to rewriteManifestUrisForProxy(rawText, targetProxyUrl)
                            }
                        }
                    }
                }
            }
        }

        return primaryCode to primaryBody
    }

    /**
     * Performs network validation of `StreamResolution.hlsProxyUrl` and inspects the manifest.
     */
    suspend fun validateAndInspectResolution(
        okHttpClient: OkHttpClient,
        resolution: StreamResolution
    ): HlsManifestValidationResult = withContext(Dispatchers.IO) {
        if (resolution.iframeOnly || resolution.playbackMode.equals("iframe", ignoreCase = true)) {
            return@withContext HlsManifestValidationResult.Invalid(
                failureTitle = "Iframe-Only Provider Stream",
                exactFailureReason = "Episode ${resolution.episode} only provides an embedded iframe player (iframeOnly=true) without a downloadable HLS stream."
            )
        }

        val proxyUrl = resolution.hlsProxyUrl?.takeIf { it.isNotBlank() }
            ?: return@withContext HlsManifestValidationResult.Invalid(
                failureTitle = "Missing hlsProxyUrl",
                exactFailureReason = "The AniScroll watch API response for Episode ${resolution.episode} did not include an hlsProxyUrl field."
            )

        if (!proxyUrl.startsWith("https://")) {
            return@withContext HlsManifestValidationResult.Invalid(
                failureTitle = "Invalid hlsProxyUrl",
                exactFailureReason = "The returned hlsProxyUrl ($proxyUrl) is not a valid HTTPS HLS URL."
            )
        }

        val headers = buildAuthorizedHeaders(resolution.refererUrl)

        try {
            val (code, bodyTextOrNull) = fetchManifestWith429Fallback(
                okHttpClient = okHttpClient,
                targetProxyUrl = proxyUrl,
                headers = headers
            )
            if (code !in 200..299 || bodyTextOrNull == null) {
                val exactReason = explainHttpFailureCode(code, proxyUrl)
                return@withContext HlsManifestValidationResult.Invalid(
                    failureTitle = "HLS Proxy Error (HTTP $code)",
                    exactFailureReason = exactReason
                )
            }

            val bodyText = bodyTextOrNull
            val parsed = parseAndValidateManifestText(
                hlsProxyUrl = proxyUrl,
                refererUrl = resolution.refererUrl,
                rawManifest = bodyText
            )

            if (parsed is HlsManifestValidationResult.Valid && parsed.isMasterPlaylist) {
                // Verify that at least one child variant playlist can be fetched
                // so we catch segment/variant restrictions or DRM upfront instead of showing fake progress.
                val firstChildLine = bodyText.lines()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }

                if (firstChildLine != null) {
                    val childUrl = resolveAgainstHlsProxyUrl(proxyUrl, firstChildLine)
                    val (childCode, childBodyOrNull) = fetchManifestWith429Fallback(
                        okHttpClient = okHttpClient,
                        targetProxyUrl = childUrl,
                        headers = headers
                    )
                    if (childCode !in 200..299 || childBodyOrNull == null) {
                        return@withContext HlsManifestValidationResult.Invalid(
                            failureTitle = "HLS Variant Playlist Unavailable (HTTP $childCode)",
                            exactFailureReason = "The master playlist loaded from hlsProxyUrl, but requesting the quality variant playlist failed (HTTP $childCode). ${explainHttpFailureCode(childCode, childUrl)}"
                        )
                    }
                    val childDrm = detectDrmRestriction(childBodyOrNull)
                    if (childDrm != null) {
                        return@withContext HlsManifestValidationResult.Invalid(
                            failureTitle = "DRM-Protected Variant Stream",
                            exactFailureReason = childDrm
                        )
                    }
                }
            }

            parsed
        } catch (e: Exception) {
            HlsManifestValidationResult.Invalid(
                failureTitle = "HLS Proxy Connection Failed",
                exactFailureReason = "Failed to connect to hlsProxyUrl: ${e.localizedMessage ?: "Network timeout or unreachable proxy server"}."
            )
        }
    }

    fun explainHttpFailureCode(httpCode: Int, url: String): String = when (httpCode) {
        401, 403 ->
            "Access denied by HLS proxy or upstream CDN (HTTP $httpCode). The signed stream token may have expired or the provider restricts segment downloads."
        404 ->
            "The HLS playlist or segment was not found on the server (HTTP 404). The stream for this episode is currently unavailable."
        410 ->
            "The HLS stream URL has expired (HTTP 410 Gone). Please re-inspect the episode to obtain a fresh hlsProxyUrl."
        429 ->
            "Too many requests to the HLS proxy (HTTP 429 Rate Limited). Please wait before starting another download."
        in 500..599 ->
            "The AniScroll HLS proxy encountered an upstream server failure (HTTP $httpCode) while fetching $url."
        else ->
            "The HLS proxy returned unexpected HTTP status $httpCode while requesting the HLS stream."
    }
}
