package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.core.database.AniScrollDatabase
import com.example.core.image.AniImageLoader
import com.example.core.network.NetworkClient
import com.example.data.anilist.AniListService
import com.example.data.aniscroll.AniScrollService
import com.example.data.kitsu.KitsuService
import com.example.data.local.UserPreferencesDataStore
import com.example.data.repository.AniListRepositoryImpl
import com.example.data.repository.AniScrollRepositoryImpl
import com.example.data.repository.AnimeRepositoryImpl
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.KitsuRepositoryImpl
import com.example.data.repository.LocalBackupManager
import com.example.data.repository.ScheduleRepositoryImpl
import com.example.data.repository.WatchlistRepositoryImpl
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AniScrollRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.KitsuRepository
import com.example.domain.repository.ScheduleRepository
import com.example.domain.repository.WatchlistRepository
import com.example.feature.notifications.NotificationScheduler
import com.example.playback.download.Media3DownloadStack
import com.example.playback.download.OfflineDownloadRepository
import okhttp3.OkHttpClient

class AppContainer(context: Application) {
    val okHttpClient: OkHttpClient = NetworkClient.createOkHttpClient(context)
    val database: AniScrollDatabase = AniScrollDatabase.getInstance(context)
    val dao = database.dao()

    val preferencesDataStore = UserPreferencesDataStore(context)
    val backupManager = LocalBackupManager(dao)
    val notificationScheduler = NotificationScheduler(context)
    val media3DownloadStack = Media3DownloadStack(context)

    private val aniListService: AniListService = NetworkClient.createService(
        baseUrl = "https://graphql.anilist.co/",
        okHttpClient = okHttpClient,
        serviceClass = AniListService::class.java
    )

    private val kitsuService: KitsuService = NetworkClient.createService(
        baseUrl = "https://kitsu.io/api/edge/",
        okHttpClient = okHttpClient,
        serviceClass = KitsuService::class.java
    )

    private val aniScrollService: AniScrollService = NetworkClient.createService(
        baseUrl = "https://api.aniscroll.in/",
        okHttpClient = okHttpClient,
        serviceClass = AniScrollService::class.java
    )

    val aniListRepository: AniListRepository = AniListRepositoryImpl(aniListService, dao)
    val kitsuRepository: KitsuRepository = KitsuRepositoryImpl(kitsuService, dao)
    val aniScrollRepository: AniScrollRepository = AniScrollRepositoryImpl(aniScrollService)

    val animeRepository: AnimeRepository = AnimeRepositoryImpl(
        aniListRepository = aniListRepository,
        kitsuRepository = kitsuRepository,
        dao = dao
    )

    val watchlistRepository: WatchlistRepository = WatchlistRepositoryImpl(dao)
    val historyRepository: HistoryRepository = HistoryRepositoryImpl(dao)
    val scheduleRepository: ScheduleRepository = ScheduleRepositoryImpl(aniListRepository)

    val offlineDownloadRepository: OfflineDownloadRepository = OfflineDownloadRepository(
        appContext = context,
        dao = dao,
        aniScrollRepository = aniScrollRepository,
        okHttpClient = okHttpClient,
        media3DownloadStack = media3DownloadStack
    )
}

class AniScrollApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationScheduler.createNotificationChannels()
    }

    override fun newImageLoader(): ImageLoader {
        return AniImageLoader.create(this, container.okHttpClient)
    }
}
