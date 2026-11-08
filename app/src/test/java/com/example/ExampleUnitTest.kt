package com.example

import com.example.core.common.TimeUtils
import com.example.data.aniscroll.AniScrollResponseAdapter
import com.example.data.kitsu.KitsuAnimeAttributes
import com.example.data.kitsu.KitsuAnimeResource
import com.example.data.kitsu.KitsuImageSetDto
import com.example.data.kitsu.KitsuMatcher
import com.example.data.kitsu.KitsuTitlesDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `AniScrollResponseAdapter parses live megaplay response and filters out thumbnails vtt`() {
        val rawResponse = """
            {
              "anilistId": 16498,
              "malId": 16498,
              "title": "Attack on Titan",
              "episode": 1,
              "type": "sub",
              "source": "megaplay",
              "server": "hd-2",
              "availableServers": ["hd-2"],
              "embedUrl": "https://megaplay.buzz/stream/s-2/214203/sub",
              "m3u8": "https://s9.SnapStorm.site/v74/master.m3u8",
              "hlsProxyUrl": "https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2Fmaster.m3u8",
              "playbackMode": "m3u8",
              "iframeOnly": false,
              "subtitles": [
                {
                  "url": "https://s.megastatics.com/subtitle/eng-0.vtt",
                  "lang": "English",
                  "default": true
                },
                {
                  "url": "https://s.megastatics.com/subtitle/jpn-4.vtt",
                  "lang": "Japanese",
                  "default": false
                },
                {
                  "url": "https://sy4.slikack.pro/Xy23/preview/thumbnails.vtt",
                  "lang": "Unknown",
                  "default": false
                }
              ],
              "intro": { "start": 95, "end": 185 },
              "outro": { "start": 1340, "end": 1419 },
              "note": "ok"
            }
        """.trimIndent()

        val resolution = AniScrollResponseAdapter.parse(
            root = json.parseToJsonElement(rawResponse).jsonObject,
            requestedAnilistId = 16498,
            requestedEpisode = 1,
            requestedAudioType = "sub"
        )

        assertEquals(16498, resolution.anilistId)
        assertEquals(1, resolution.episode)
        assertEquals("sub", resolution.audioType)
        assertEquals("https://s9.SnapStorm.site/v74/master.m3u8", resolution.hlsUrl)
        assertTrue(resolution.fallbackDirectM3u8Url?.contains("api.aniscroll.in/api/proxy/hls") == true)
        assertEquals(2, resolution.subtitles.size)
        assertTrue(resolution.subtitles.none { it.url.contains("thumbnails.vtt") })
        assertEquals("English", resolution.subtitles.first().lang)
        assertTrue(resolution.subtitles.first().isDefault)
        assertEquals(95, resolution.introStartSec)
        assertEquals(185, resolution.introEndSec)
        assertEquals("https://megaplay.buzz/stream/s-2/214203/sub", resolution.refererUrl)
    }

    @Test
    fun `KitsuMatcher matches exact title and year while rejecting mismatched sequel`() {
        val candidates = listOf(
            KitsuAnimeResource(
                id = "7442",
                attributes = KitsuAnimeAttributes(
                    slug = "attack-on-titan",
                    canonicalTitle = "Attack on Titan",
                    titles = KitsuTitlesDto(
                        en = "Attack on Titan",
                        en_jp = "Shingeki no Kyojin",
                        ja_jp = "進撃の巨人"
                    ),
                    startDate = "2013-04-07",
                    posterImage = KitsuImageSetDto(original = "https://media.kitsu.app/poster_2013.jpg"),
                    coverImage = KitsuImageSetDto(original = "https://media.kitsu.app/cover_2013.jpg")
                )
            ),
            KitsuAnimeResource(
                id = "42950",
                attributes = KitsuAnimeAttributes(
                    slug = "attack-on-titan-the-final-season",
                    canonicalTitle = "Attack on Titan: The Final Season",
                    titles = KitsuTitlesDto(
                        en = "Attack on Titan: The Final Season",
                        en_jp = "Shingeki no Kyojin: The Final Season"
                    ),
                    startDate = "2020-12-07",
                    posterImage = KitsuImageSetDto(original = "https://media.kitsu.app/poster_2020.jpg")
                )
            )
        )

        val matched = KitsuMatcher.findVerifiedMatch(
            romajiTitle = "Shingeki no Kyojin",
            englishTitle = "Attack on Titan",
            nativeTitle = "進撃の巨人",
            releaseYear = 2013,
            candidates = candidates
        )
        assertNotNull(matched)
        assertEquals("7442", matched?.id)

        val mismatchedYear = KitsuMatcher.findVerifiedMatch(
            romajiTitle = "Mataku Chigau Anime",
            englishTitle = "Completely Different Anime",
            nativeTitle = null,
            releaseYear = 1999,
            candidates = candidates
        )
        assertNull(mismatchedYear)
    }

    @Test
    fun `TimeUtils formats player durations and strips HTML tags cleanly`() {
        assertEquals("01:35", TimeUtils.formatDurationMs(95_000L))
        assertEquals("01:02:05", TimeUtils.formatDurationMs(3_725_000L))

        val dirtyHtml = "<p>First paragraph.<br><br><i>Italic note</i> &amp; <b>bold</b></p>"
        val clean = TimeUtils.stripHtml(dirtyHtml)
        assertFalse(clean.contains("<p>"))
        assertTrue(clean.contains("First paragraph."))
        assertTrue(clean.contains("& bold"))
    }

    @Test
    fun `VideoDisplayMode contains only Fit Screen, Zoom to Fill, Stretch, and Original and maps to Media3 resizeModes`() {
        val modes = com.example.domain.model.VideoDisplayMode.entries
        assertEquals(4, modes.size)
        assertEquals(
            listOf("Fit Screen", "Zoom to Fill", "Stretch", "Original"),
            modes.map { it.displayName }
        )

        // Fit Screen -> RESIZE_MODE_FIT
        assertEquals(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
            com.example.playback.engine.VideoDisplayModeMapper.toMedia3ResizeMode(
                com.example.domain.model.VideoDisplayMode.FIT
            )
        )

        // Zoom to Fill -> RESIZE_MODE_ZOOM
        assertEquals(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            com.example.playback.engine.VideoDisplayModeMapper.toMedia3ResizeMode(
                com.example.domain.model.VideoDisplayMode.FILL
            )
        )
        assertEquals(
            androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
            com.example.playback.engine.VideoDisplayModeMapper.toMedia3VideoScalingMode(
                com.example.domain.model.VideoDisplayMode.FILL
            )
        )

        // Stretch -> RESIZE_MODE_FILL
        assertEquals(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL,
            com.example.playback.engine.VideoDisplayModeMapper.toMedia3ResizeMode(
                com.example.domain.model.VideoDisplayMode.STRETCH
            )
        )

        // Original -> RESIZE_MODE_FIT
        assertEquals(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
            com.example.playback.engine.VideoDisplayModeMapper.toMedia3ResizeMode(
                com.example.domain.model.VideoDisplayMode.ORIGINAL
            )
        )

        // Legacy stored values map cleanly to FILL (Zoom to Fill)
        assertEquals(
            com.example.domain.model.VideoDisplayMode.FILL,
            com.example.domain.model.VideoDisplayMode.fromNameOrDefault("ZOOM")
        )
        assertEquals(
            com.example.domain.model.VideoDisplayMode.FILL,
            com.example.domain.model.VideoDisplayMode.fromNameOrDefault("Zoom to Fill")
        )
    }

    @Test
    fun `HlsManifestValidator parses master playlist and enables only available 360p 480p 720p 1080p variants`() {
        val masterM3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-STREAM-INF:BANDWIDTH=850000,RESOLUTION=640x360,CODECS="avc1.4d401f,mp4a.40.2"
            360p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2400000,RESOLUTION=1280x720,CODECS="avc1.4d401f,mp4a.40.2"
            720p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=4800000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
            1080p/index.m3u8
        """.trimIndent()

        val proxyUrl = "https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2Fmaster.m3u8"
        val result = com.example.playback.download.HlsManifestValidator.parseAndValidateManifestText(
            hlsProxyUrl = proxyUrl,
            refererUrl = "https://megaplay.buzz/",
            rawManifest = masterM3u8
        )

        assertTrue(result is com.example.playback.download.HlsManifestValidationResult.Valid)
        val valid = result as com.example.playback.download.HlsManifestValidationResult.Valid
        assertTrue(valid.isMasterPlaylist)
        assertEquals(4, valid.variants.size)

        val mapByLabel = valid.variants.associateBy { it.qualityLabel }
        assertTrue(mapByLabel.getValue("360p").isAvailable)
        assertFalse(mapByLabel.getValue("480p").isAvailable) // 480p not in master playlist!
        assertTrue(mapByLabel.getValue("720p").isAvailable)
        assertTrue(mapByLabel.getValue("1080p").isAvailable)

        // Verify relative variant URI is routed through the AniScroll HLS proxy
        val resolved720Url = com.example.playback.download.HlsManifestValidator.resolveAgainstHlsProxyUrl(
            baseProxyUrl = proxyUrl,
            childUri = "720p/index.m3u8"
        )
        assertTrue(resolved720Url.startsWith("https://api.aniscroll.in/api/proxy/hls?url="))
        assertTrue(resolved720Url.contains("720p%2Findex.m3u8"))
    }

    @Test
    fun `HlsManifestValidator rejects DRM protected playlists and non-HLS responses with exact failure reason`() {
        val drmManifest = """
            #EXTM3U
            #EXT-X-VERSION:5
            #EXT-X-KEY:METHOD=SAMPLE-AES,URI="skd://drm.provider.example/key",KEYFORMAT="com.apple.streamingkeydelivery"
            #EXT-X-STREAM-INF:BANDWIDTH=2400000,RESOLUTION=1280x720
            720p/index.m3u8
        """.trimIndent()

        val drmResult = com.example.playback.download.HlsManifestValidator.parseAndValidateManifestText(
            hlsProxyUrl = "https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fcdn.example.com%2Fmaster.m3u8",
            refererUrl = "https://megaplay.buzz/",
            rawManifest = drmManifest
        )

        assertTrue(drmResult is com.example.playback.download.HlsManifestValidationResult.Invalid)
        val invalidDrm = drmResult as com.example.playback.download.HlsManifestValidationResult.Invalid
        assertTrue(invalidDrm.exactFailureReason.contains("DRM", ignoreCase = true) || invalidDrm.failureTitle.contains("DRM", ignoreCase = true))

        val htmlErrorBody = "<html><body>403 Forbidden - Proxy token expired</body></html>"
        val nonHlsResult = com.example.playback.download.HlsManifestValidator.parseAndValidateManifestText(
            hlsProxyUrl = "https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fcdn.example.com%2Fmaster.m3u8",
            refererUrl = "https://megaplay.buzz/",
            rawManifest = htmlErrorBody
        )

        assertTrue(nonHlsResult is com.example.playback.download.HlsManifestValidationResult.Invalid)
        val invalidHtml = nonHlsResult as com.example.playback.download.HlsManifestValidationResult.Invalid
        assertTrue(invalidHtml.exactFailureReason.contains("#EXTM3U"))
    }

    @Test
    fun `HlsManifestValidator rewriteManifestUrisForProxy rewrites relative segment and init map URIs to canonical hlsProxyUrl`() {
        val variantProxyUrl = "https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2F720p%2Findex.m3u8"
        val upstreamMediaPlaylist = """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-TARGETDURATION:4
            #EXT-X-MAP:URI="init-v1.mp4"
            #EXTINF:4.000,
            seg-001.ts
            #EXTINF:4.000,
            seg-002.ts
        """.trimIndent()

        val rewritten = com.example.playback.download.HlsManifestValidator.rewriteManifestUrisForProxy(
            rawManifest = upstreamMediaPlaylist,
            baseProxyUrl = variantProxyUrl
        )

        assertTrue(rewritten.contains("URI=\"https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2F720p%2Finit-v1.mp4\""))
        assertTrue(rewritten.contains("https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2F720p%2Fseg-001.ts"))
        assertTrue(rewritten.contains("https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fs9.SnapStorm.site%2Fv74%2F720p%2Fseg-002.ts"))
    }

    @Test
    fun `NetworkConnectivityObserver evaluateCapabilities requires both internet and validated capabilities`() {
        assertTrue(
            com.example.core.network.NetworkConnectivityObserver.evaluateCapabilities(
                hasInternetCapability = true,
                hasValidatedCapability = true
            )
        )
        assertFalse(
            com.example.core.network.NetworkConnectivityObserver.evaluateCapabilities(
                hasInternetCapability = true,
                hasValidatedCapability = false
            )
        )
        assertFalse(
            com.example.core.network.NetworkConnectivityObserver.evaluateCapabilities(
                hasInternetCapability = false,
                hasValidatedCapability = true
            )
        )
        assertFalse(
            com.example.core.network.NetworkConnectivityObserver.evaluateCapabilities(
                hasInternetCapability = false,
                hasValidatedCapability = false
            )
        )
    }

    @Test
    fun `OfflineDownloadItem marks only COMPLETED items playable offline and partitions Downloaded vs Downloading tabs`() {
        fun sampleItem(id: String, state: com.example.domain.model.OfflineDownloadState, percent: Float) =
            com.example.domain.model.OfflineDownloadItem(
                downloadId = id,
                anilistId = 154587,
                episode = 1,
                totalEpisodes = 28,
                audioType = "sub",
                animeTitle = "Frieren: Beyond Journey's End",
                posterUrl = "https://example.com/poster.jpg",
                coverUrl = "https://example.com/cover.jpg",
                qualityLabel = "720p",
                videoHeight = 720,
                hlsProxyUrl = "https://api.aniscroll.in/api/proxy/hls?url=https%3A%2F%2Fcdn.example.com%2Fmaster.m3u8",
                refererUrl = "https://megaplay.buzz/",
                state = state,
                progressPercent = percent,
                bytesDownloaded = 128_000_000L,
                contentLengthBytes = 256_000_000L,
                failureReasonText = null,
                localSubtitleTracks = emptyList(),
                createdAtMs = 1000L,
                updatedAtMs = 2000L
            )

        val completed = sampleItem("ep1", com.example.domain.model.OfflineDownloadState.COMPLETED, 100f)
        val downloading = sampleItem("ep2", com.example.domain.model.OfflineDownloadState.DOWNLOADING, 65f)
        val paused = sampleItem("ep3", com.example.domain.model.OfflineDownloadState.PAUSED, 40f)
        val queued = sampleItem("ep4", com.example.domain.model.OfflineDownloadState.QUEUED, 0f)
        val failed = sampleItem("ep5", com.example.domain.model.OfflineDownloadState.FAILED, 12f)

        assertTrue(completed.isPlayableOffline)
        assertFalse(downloading.isPlayableOffline)
        assertFalse(paused.isPlayableOffline)
        assertFalse(queued.isPlayableOffline)
        assertFalse(failed.isPlayableOffline)

        val allItems = listOf(completed, downloading, paused, queued, failed)
        val downloadedTab = allItems.filter { it.isPlayableOffline }
        val downloadingTab = allItems.filter { !it.isPlayableOffline }

        assertEquals(listOf("ep1"), downloadedTab.map { it.downloadId })
        assertEquals(listOf("ep2", "ep3", "ep4", "ep5"), downloadingTab.map { it.downloadId })
    }
}

