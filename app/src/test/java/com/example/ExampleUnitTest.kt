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
}
