package com.example.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AniScrollDao {
    // Anime Metadata
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnime(anime: AnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeList(list: List<AnimeEntity>)

    @Query("SELECT * FROM anime_metadata WHERE anilistId = :anilistId LIMIT 1")
    suspend fun getAnimeById(anilistId: Int): AnimeEntity?

    @Query("SELECT * FROM anime_metadata WHERE anilistId IN (:ids)")
    suspend fun getAnimeByIds(ids: List<Int>): List<AnimeEntity>

    @Query("SELECT * FROM anime_metadata ORDER BY popularity DESC LIMIT :limit")
    suspend fun getCachedPopularAnime(limit: Int = 30): List<AnimeEntity>

    // Artwork Mapping (AniList -> Kitsu)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtwork(artwork: AnimeArtworkEntity)

    @Query("SELECT * FROM anime_artwork WHERE anilistId = :anilistId LIMIT 1")
    suspend fun getArtwork(anilistId: Int): AnimeArtworkEntity?

    // Watchlist
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE anilistId = :anilistId")
    suspend fun deleteFromWatchlist(anilistId: Int)

    @Query("SELECT * FROM watchlist ORDER BY addedAtMs DESC")
    fun observeAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY addedAtMs DESC")
    suspend fun getAllWatchlistSnapshot(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE anilistId = :anilistId LIMIT 1")
    fun observeWatchlistById(anilistId: Int): Flow<WatchlistEntity?>

    @Query("SELECT * FROM watchlist WHERE anilistId = :anilistId LIMIT 1")
    suspend fun getWatchlistById(anilistId: Int): WatchlistEntity?

    // Favorites
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE anilistId = :anilistId")
    suspend fun deleteFavorite(anilistId: Int)

    @Query("SELECT * FROM favorites")
    fun observeAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavoritesSnapshot(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE anilistId = :anilistId)")
    fun observeIsFavorite(anilistId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE anilistId = :anilistId)")
    suspend fun isFavorite(anilistId: Int): Boolean

    // Watch Progress & Watch History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchProgress(progress: WatchProgressEntity)

    @Query("SELECT * FROM watch_progress WHERE anilistId = :anilistId AND episode = :episode LIMIT 1")
    suspend fun getWatchProgress(anilistId: Int, episode: Int): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAtMs DESC")
    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAtMs DESC")
    suspend fun getAllHistorySnapshot(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE isCompleted = 0 AND positionMs > 0 ORDER BY lastWatchedAtMs DESC LIMIT 15")
    fun observeContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE anilistId = :anilistId")
    fun observeHistoryForAnime(anilistId: Int): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE anilistId = :anilistId AND episode = :episode")
    suspend fun deleteHistoryItem(anilistId: Int, episode: Int)

    @Query("DELETE FROM watch_progress WHERE anilistId = :anilistId AND episode = :episode")
    suspend fun deleteProgressItem(anilistId: Int, episode: Int)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllWatchHistory()

    @Query("DELETE FROM watch_progress")
    suspend fun clearAllWatchProgress()

    // Episodes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE anilistId = :anilistId ORDER BY episodeNumber ASC")
    suspend fun getEpisodesForAnime(anilistId: Int): List<EpisodeEntity>

    // Schedule
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItems(items: List<ScheduleEntity>)

    @Query("SELECT * FROM schedule WHERE airingAtEpoch BETWEEN :startEpoch AND :endEpoch ORDER BY airingAtEpoch ASC")
    suspend fun getScheduleBetween(startEpoch: Long, endEpoch: Long): List<ScheduleEntity>

    // Recent Searches
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY searchedAtMs DESC LIMIT 15")
    fun observeRecentSearches(): Flow<List<RecentSearchEntity>>

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    // Personal Ratings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRating(rating: PersonalRatingEntity)

    @Query("SELECT * FROM personal_ratings WHERE anilistId = :anilistId LIMIT 1")
    fun observePersonalRating(anilistId: Int): Flow<PersonalRatingEntity?>

    @Query("SELECT * FROM personal_ratings")
    suspend fun getAllRatingsSnapshot(): List<PersonalRatingEntity>

    // Clear all user data
    @Query("DELETE FROM watchlist")
    suspend fun clearWatchlist()

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("DELETE FROM personal_ratings")
    suspend fun clearPersonalRatings()

    @Query("DELETE FROM anime_metadata")
    suspend fun clearMetadataCache()

    @Query("DELETE FROM anime_artwork")
    suspend fun clearArtworkCache()
}

@Database(
    entities = [
        AnimeEntity::class,
        AnimeArtworkEntity::class,
        WatchProgressEntity::class,
        WatchHistoryEntity::class,
        WatchlistEntity::class,
        FavoriteEntity::class,
        EpisodeEntity::class,
        ScheduleEntity::class,
        RecentSearchEntity::class,
        PersonalRatingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AniScrollDatabase : RoomDatabase() {
    abstract fun dao(): AniScrollDao

    companion object {
        @Volatile
        private var INSTANCE: AniScrollDatabase? = null

        fun getInstance(context: Context): AniScrollDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AniScrollDatabase::class.java,
                    "aniscroll_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
