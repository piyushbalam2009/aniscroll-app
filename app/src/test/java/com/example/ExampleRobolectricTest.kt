package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AniScrollDatabase
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.LocalBackupManager
import com.example.data.repository.WatchlistRepositoryImpl
import com.example.domain.model.Anime
import com.example.domain.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AniScrollDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AniScrollDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app_name resource matches AniScroll`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AniScroll", appName)
    }

    @Test
    fun `WatchlistRepository toggles and updates local watchlist states`() = runBlocking {
        val watchlistRepo = WatchlistRepositoryImpl(database.dao())
        val sampleAnime = Anime(
            anilistId = 16498,
            englishTitle = "Attack on Titan",
            romajiTitle = "Shingeki no Kyojin",
            nativeTitle = "進撃の巨人",
            description = "Humanity fights Titans.",
            posterUrl = "https://example.com/poster.jpg",
            coverUrl = "https://example.com/banner.jpg",
            genres = listOf("Action", "Drama"),
            score = 8.5,
            releaseYear = 2013,
            format = "TV",
            status = "FINISHED",
            totalEpisodes = 25
        )

        watchlistRepo.toggleWatchlist(sampleAnime)

        val idsAfterAdd = watchlistRepo.observeWatchlistIds().first()
        assertTrue(idsAfterAdd.contains(16498))

        watchlistRepo.addOrUpdateWatchlist(sampleAnime, WatchStatus.COMPLETED)
        val items = watchlistRepo.observeWatchlist().first()
        assertEquals(1, items.size)
        assertEquals(WatchStatus.COMPLETED, items.first().status)

        watchlistRepo.toggleWatchlist(sampleAnime)
        assertTrue(watchlistRepo.observeWatchlist().first().isEmpty())
    }

    @Test
    fun `HistoryRepository saves progress and protects valid position from zero overwrite`() = runBlocking {
        val historyRepo = HistoryRepositoryImpl(database.dao())
        val sampleAnime = Anime(
            anilistId = 21,
            englishTitle = "One Piece",
            romajiTitle = "One Piece",
            nativeTitle = "ONE PIECE",
            description = "Pirate adventure.",
            posterUrl = "https://example.com/op.jpg",
            coverUrl = "https://example.com/op_banner.jpg",
            genres = listOf("Action", "Adventure"),
            score = 8.8,
            releaseYear = 1999,
            format = "TV",
            status = "RELEASING",
            totalEpisodes = 1100
        )

        historyRepo.savePlaybackProgress(
            anime = sampleAnime,
            anilistId = 21,
            animeTitle = sampleAnime.displayTitle,
            posterUrl = sampleAnime.posterUrl,
            coverUrl = sampleAnime.coverUrl,
            episode = 1,
            totalEpisodes = 1100,
            audioType = "sub",
            positionMs = 600_000L,
            durationMs = 1_440_000L
        )

        // Attempt zero position overwrite (e.g. player teardown before seek)
        historyRepo.savePlaybackProgress(
            anime = sampleAnime,
            anilistId = 21,
            animeTitle = sampleAnime.displayTitle,
            posterUrl = sampleAnime.posterUrl,
            coverUrl = sampleAnime.coverUrl,
            episode = 1,
            totalEpisodes = 1100,
            audioType = "sub",
            positionMs = 0L,
            durationMs = 1_440_000L
        )

        val savedPos = historyRepo.getSavedPositionMs(21, 1)
        assertEquals(600_000L, savedPos)

        val continueWatching = historyRepo.observeContinueWatching().first()
        assertEquals(1, continueWatching.size)
        assertEquals(21, continueWatching.first().anilistId)
        assertFalse(continueWatching.first().isCompleted)
    }

    @Test
    fun `LocalBackupManager exports and restores JSON backup cleanly`() = runBlocking {
        val dao = database.dao()
        val watchlistRepo = WatchlistRepositoryImpl(dao)
        val backupManager = LocalBackupManager(dao)

        val sampleAnime = Anime(
            anilistId = 1535,
            englishTitle = "Death Note",
            romajiTitle = "Death Note",
            nativeTitle = "DEATH NOTE",
            description = "Shinigami notebook.",
            posterUrl = "https://example.com/dn.jpg",
            coverUrl = null,
            genres = listOf("Mystery", "Thriller"),
            score = 8.4,
            releaseYear = 2006,
            format = "TV",
            status = "FINISHED",
            totalEpisodes = 37
        )
        watchlistRepo.addOrUpdateWatchlist(sampleAnime, WatchStatus.WATCHING)

        val exportedJson = backupManager.exportJson()
        assertTrue(exportedJson.contains("Death Note"))

        dao.clearWatchlist()
        assertTrue(watchlistRepo.observeWatchlist().first().isEmpty())

        val restored = backupManager.importJson(exportedJson)
        assertTrue(restored)
        val restoredItems = watchlistRepo.observeWatchlist().first()
        assertEquals(1, restoredItems.size)
        assertEquals(1535, restoredItems.first().anilistId)
    }
}
