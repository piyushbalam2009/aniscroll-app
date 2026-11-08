package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.common.AppResult
import com.example.core.navigation.MainBottomTab
import com.example.core.navigation.ScreenRoute
import com.example.domain.model.Anime
import com.example.feature.common.MainViewModel
import com.example.feature.details.AnimeDetailsScreen
import com.example.feature.discover.NewEpisodeReleasesScreen
import com.example.feature.discover.TopHitsScreen
import com.example.feature.history.DownloadsScreen
import com.example.feature.history.HistoryScreen
import com.example.feature.home.HomeScreen
import com.example.feature.movies.MovieDetailsScreen
import com.example.feature.mylist.MyListScreen
import com.example.feature.notifications.NotificationsScreen
import com.example.feature.schedule.ReleaseCalendarScreen
import com.example.feature.search.SearchScreen
import com.example.feature.search.SortAndFilterScreen
import com.example.feature.settings.HelpCenterScreen
import com.example.feature.settings.PrivacyPolicyScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.splash.ChooseInterestScreen
import com.example.feature.splash.SplashScreen
import com.example.feature.splash.WelcomeScreen
import com.example.playback.controls.VideoPlayerScreen
import com.example.playback.engine.VideoPlayerViewModel
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniScrollTheme
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniTextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (applicationContext as AniScrollApp).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(appContainer)
            )
            val preferences by mainViewModel.preferences.collectAsState()

            AniScrollTheme(darkTheme = preferences.darkTheme) {
                AniScrollAppContent(
                    appContainer = appContainer,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}

@Composable
fun AniScrollAppContent(
    appContainer: AppContainer,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val preferences by mainViewModel.preferences.collectAsState()
    val isAppReady by mainViewModel.isAppReady.collectAsState()
    val homeState by mainViewModel.homeState.collectAsState()
    var activeBottomTab by rememberSaveable { mutableStateOf(MainBottomTab.HOME) }

    val navigateToAnime: (Anime) -> Unit = { anime ->
        if (anime.isMovie) {
            navController.navigate(ScreenRoute.MovieDetails(anime.anilistId))
        } else {
            navController.navigate(ScreenRoute.AnimeDetails(anime.anilistId))
        }
    }

    val navigateToPlayer: (Int, Int, String, String, Int) -> Unit = { anilistId, episode, audioType, title, totalEps ->
        navController.navigate(
            ScreenRoute.VideoPlayer(
                anilistId = anilistId,
                episode = episode,
                audioType = audioType,
                title = title,
                totalEpisodes = totalEps.coerceAtLeast(1)
            )
        )
    }

    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Splash,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<ScreenRoute.Splash> {
            SplashScreen(
                isAppReady = isAppReady,
                onboardingCompleted = preferences.onboardingCompleted,
                onNavigateToWelcome = {
                    navController.navigate(ScreenRoute.Welcome) {
                        popUpTo(ScreenRoute.Splash) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(ScreenRoute.Main) {
                        popUpTo(ScreenRoute.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenRoute.Welcome> {
            WelcomeScreen(
                samplePosters = homeState.dashboard.topHits.ifEmpty { homeState.dashboard.trendingAnime },
                onGetStarted = {
                    navController.navigate(ScreenRoute.ChooseInterest)
                },
                onSkip = {
                    mainViewModel.completeOnboarding(emptySet())
                    navController.navigate(ScreenRoute.Main) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenRoute.ChooseInterest> {
            ChooseInterestScreen(
                initialInterests = preferences.selectedInterests,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(ScreenRoute.Main) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    mainViewModel.completeOnboarding(preferences.selectedInterests)
                    navController.navigate(ScreenRoute.Main) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onContinue = { selectedInterests ->
                    mainViewModel.completeOnboarding(selectedInterests)
                    navController.navigate(ScreenRoute.Main) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenRoute.Main> {
            val watchlistIds by mainViewModel.watchlistIds.collectAsState()
            val watchlist by mainViewModel.watchlist.collectAsState()
            val watchHistory by mainViewModel.watchHistory.collectAsState()
            val continueWatching by mainViewModel.continueWatching.collectAsState()
            val selectedDayOffset by mainViewModel.selectedDayOffset.collectAsState()
            val scheduleState by mainViewModel.scheduleState.collectAsState()
            val scheduledReminderIds by mainViewModel.scheduledReminderIds.collectAsState()

            BackHandler(enabled = activeBottomTab != MainBottomTab.HOME) {
                activeBottomTab = MainBottomTab.HOME
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    AniScrollBottomBar(
                        currentTab = activeBottomTab,
                        onSelectTab = { activeBottomTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = activeBottomTab, label = "main_tab_crossfade") { tab ->
                        when (tab) {
                            MainBottomTab.HOME -> {
                                HomeScreen(
                                    uiState = homeState,
                                    watchlistIds = watchlistIds,
                                    continueWatching = continueWatching,
                                    recentlyWatched = watchHistory,
                                    onSearchClick = {
                                        navController.navigate(ScreenRoute.Search())
                                    },
                                    onNotificationsClick = {
                                        navController.navigate(ScreenRoute.Notifications)
                                    },
                                    onSeeAllTopHits = {
                                        navController.navigate(ScreenRoute.TopHits)
                                    },
                                    onSeeAllNewReleases = {
                                        navController.navigate(ScreenRoute.NewReleases)
                                    },
                                    onAnimeClick = { anime ->
                                        navigateToAnime(anime)
                                    },
                                    onPlayAnime = { anime ->
                                        navigateToPlayer(
                                            anime.anilistId,
                                            1,
                                            preferences.defaultAudioType,
                                            anime.displayTitle,
                                            anime.availableEpisodeCount
                                        )
                                    },
                                    onResumeHistory = { historyItem ->
                                        navigateToPlayer(
                                            historyItem.anilistId,
                                            historyItem.episode,
                                            historyItem.audioType,
                                            historyItem.animeTitle,
                                            historyItem.totalEpisodes
                                        )
                                    },
                                    onToggleWatchlist = { anime ->
                                        mainViewModel.toggleWatchlist(anime)
                                    },
                                    onRetry = {
                                        mainViewModel.refreshHome()
                                    }
                                )
                            }

                            MainBottomTab.SCHEDULE -> {
                                ReleaseCalendarScreen(
                                    selectedDayOffset = selectedDayOffset,
                                    scheduleState = scheduleState,
                                    watchlistIds = watchlistIds,
                                    scheduledReminderIds = scheduledReminderIds,
                                    onSelectDayOffset = { mainViewModel.loadScheduleForDayOffset(it) },
                                    onAnimeClick = { anime ->
                                        navigateToAnime(anime)
                                    },
                                    onToggleWatchlist = { anime ->
                                        mainViewModel.toggleWatchlist(anime)
                                    },
                                    onToggleReminder = { scheduleItem ->
                                        mainViewModel.toggleScheduleReminder(scheduleItem)
                                    },
                                    onRetry = {
                                        mainViewModel.loadScheduleForDayOffset(selectedDayOffset)
                                    }
                                )
                            }

                            MainBottomTab.MY_LIST -> {
                                MyListScreen(
                                    watchlist = watchlist,
                                    onAnimeClick = { anime ->
                                        navigateToAnime(anime)
                                    },
                                    onSetWatchStatus = { anime, status ->
                                        mainViewModel.updateWatchlistStatus(anime, status)
                                    },
                                    onRemoveFromWatchlist = { anilistId ->
                                        mainViewModel.removeFromWatchlist(anilistId)
                                    }
                                )
                            }

                            MainBottomTab.HISTORY -> {
                                HistoryScreen(
                                    history = watchHistory,
                                    onResumeItem = { item ->
                                        navigateToPlayer(
                                            item.anilistId,
                                            item.episode,
                                            item.audioType,
                                            item.animeTitle,
                                            item.totalEpisodes.coerceAtLeast(item.episode)
                                        )
                                    },
                                    onRemoveItem = { item ->
                                        mainViewModel.removeHistoryItem(item.anilistId, item.episode)
                                    },
                                    onClearAll = {
                                        mainViewModel.clearWatchHistory()
                                    }
                                )
                            }

                            MainBottomTab.SETTINGS -> {
                                val context = LocalContext.current
                                SettingsScreen(
                                    preferences = preferences,
                                    cacheSizeText = mainViewModel.calculateCacheSizeMb(context),
                                    onOpenDownloads = {
                                        navController.navigate(ScreenRoute.Downloads)
                                    },
                                    onOpenHelpCenter = {
                                        navController.navigate(ScreenRoute.HelpCenter)
                                    },
                                    onOpenPrivacyPolicy = {
                                        navController.navigate(ScreenRoute.PrivacyPolicy)
                                    },
                                    onOpenInterests = {
                                        navController.navigate(ScreenRoute.ChooseInterest)
                                    },
                                    onSetDarkTheme = { mainViewModel.setDarkTheme(it) },
                                    onSetLanguage = { mainViewModel.setAppLanguage(it) },
                                    onSetDefaultSubtitle = { mainViewModel.setDefaultSubtitleLang(it) },
                                    onSetDefaultAudio = { mainViewModel.setDefaultAudioType(it) },
                                    onSetDefaultSpeed = { mainViewModel.setDefaultPlaybackSpeed(it) },
                                    onSetDefaultQuality = { mainViewModel.setDefaultVideoQuality(it) },
                                    onSetAutoPlayNext = { mainViewModel.setAutoPlayNextEpisode(it) },
                                    onSetRememberPosition = { mainViewModel.setRememberPlaybackPosition(it) },
                                    onUpdateNotificationPrefs = { general, airing, newEp, appUpd ->
                                        mainViewModel.updateNotificationPrefs(general, airing, newEp, appUpd)
                                    },
                                    onClearCache = {
                                        mainViewModel.clearAppCache(context)
                                    },
                                    onClearHistory = {
                                        mainViewModel.clearWatchHistory()
                                    },
                                    onClearAllData = {
                                        mainViewModel.clearAllLocalData()
                                    },
                                    onExportJson = {
                                        mainViewModel.exportLocalDataJson()
                                    },
                                    onImportJson = { rawJson ->
                                        mainViewModel.importLocalDataJson(rawJson)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        composable<ScreenRoute.TopHits> {
            val topHitsState by mainViewModel.topHitsState.collectAsState()
            val watchlistIds by mainViewModel.watchlistIds.collectAsState()
            TopHitsScreen(
                state = topHitsState,
                watchlistIds = watchlistIds,
                onBack = { navController.popBackStack() },
                onSearchClick = { navController.navigate(ScreenRoute.Search()) },
                onAnimeClick = { anime -> navigateToAnime(anime) },
                onToggleWatchlist = { anime -> mainViewModel.toggleWatchlist(anime) },
                onRetry = { mainViewModel.loadTopHits() }
            )
        }

        composable<ScreenRoute.NewReleases> {
            val newReleasesState by mainViewModel.newReleasesState.collectAsState()
            NewEpisodeReleasesScreen(
                state = newReleasesState,
                onBack = { navController.popBackStack() },
                onSearchClick = { navController.navigate(ScreenRoute.Search()) },
                onAnimeClick = { anime -> navigateToAnime(anime) },
                onRetry = { mainViewModel.loadNewReleases() }
            )
        }

        composable<ScreenRoute.Search> {
            val searchState by mainViewModel.searchState.collectAsState()
            val recentSearches by mainViewModel.recentSearches.collectAsState()
            SearchScreen(
                searchState = searchState,
                recentSearches = recentSearches,
                topSearches = homeState.dashboard.topHits.ifEmpty { homeState.dashboard.trendingAnime },
                onBack = { navController.popBackStack() },
                onQueryChange = { mainViewModel.updateSearchQuery(it) },
                onSubmitSearch = { mainViewModel.executeImmediateSearch(it) },
                onOpenSortFilter = { navController.navigate(ScreenRoute.SortFilter) },
                onDeleteRecentSearch = { mainViewModel.deleteRecentSearch(it) },
                onClearRecentSearches = { mainViewModel.clearRecentSearches() },
                onAnimeClick = { anime -> navigateToAnime(anime) }
            )
        }

        composable<ScreenRoute.SortFilter> {
            val searchState by mainViewModel.searchState.collectAsState()
            SortAndFilterScreen(
                initialFilter = searchState.filter,
                onBack = { navController.popBackStack() },
                onReset = {
                    mainViewModel.resetFilterState()
                    navController.popBackStack()
                },
                onApply = { updatedFilter ->
                    mainViewModel.applyFilterState(updatedFilter)
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenRoute.AnimeDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<ScreenRoute.AnimeDetails>()
            val detailsMap by mainViewModel.animeDetailsMap.collectAsState()
            val watchlistIds by mainViewModel.watchlistIds.collectAsState()
            val episodeProgressMap by mainViewModel.observeEpisodeProgress(route.anilistId)
                .collectAsState(initial = emptyMap())
            val personalRating by mainViewModel.observePersonalRating(route.anilistId)
                .collectAsState(initial = null)

            LaunchedEffect(route.anilistId) {
                mainViewModel.loadAnimeDetails(route.anilistId, forceRefresh = false)
            }

            AnimeDetailsScreen(
                state = detailsMap[route.anilistId] ?: AppResult.Loading,
                inWatchlist = watchlistIds.contains(route.anilistId),
                episodeProgressMap = episodeProgressMap,
                personalRating = personalRating,
                defaultAudioType = preferences.defaultAudioType,
                onBack = { navController.popBackStack() },
                onPlayEpisode = { ep, audioType, title, totalEps ->
                    navigateToPlayer(route.anilistId, ep, audioType, title, totalEps)
                },
                onToggleWatchlist = { anime -> mainViewModel.toggleWatchlist(anime) },
                onSetWatchStatus = { anime, status ->
                    mainViewModel.updateWatchlistStatus(anime, status)
                },
                onSaveRating = { stars ->
                    mainViewModel.savePersonalRating(route.anilistId, stars)
                },
                onAnimeClick = { relatedAnime ->
                    navigateToAnime(relatedAnime)
                },
                onRetry = {
                    mainViewModel.loadAnimeDetails(route.anilistId, forceRefresh = true)
                }
            )
        }

        composable<ScreenRoute.MovieDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<ScreenRoute.MovieDetails>()
            val detailsMap by mainViewModel.animeDetailsMap.collectAsState()
            val watchlistIds by mainViewModel.watchlistIds.collectAsState()
            val personalRating by mainViewModel.observePersonalRating(route.anilistId)
                .collectAsState(initial = null)

            LaunchedEffect(route.anilistId) {
                mainViewModel.loadAnimeDetails(route.anilistId, forceRefresh = false)
            }

            MovieDetailsScreen(
                state = detailsMap[route.anilistId] ?: AppResult.Loading,
                inWatchlist = watchlistIds.contains(route.anilistId),
                personalRating = personalRating,
                defaultAudioType = preferences.defaultAudioType,
                onBack = { navController.popBackStack() },
                onPlayMovie = { audioType, title ->
                    navigateToPlayer(route.anilistId, 1, audioType, title, 1)
                },
                onToggleWatchlist = { anime -> mainViewModel.toggleWatchlist(anime) },
                onSetWatchStatus = { anime, status ->
                    mainViewModel.updateWatchlistStatus(anime, status)
                },
                onSaveRating = { stars -> mainViewModel.savePersonalRating(route.anilistId, stars) },
                onAnimeClick = { related -> navigateToAnime(related) },
                onRetry = {
                    mainViewModel.loadAnimeDetails(route.anilistId, forceRefresh = true)
                }
            )
        }

        composable<ScreenRoute.VideoPlayer> { backStackEntry ->
            val route = backStackEntry.toRoute<ScreenRoute.VideoPlayer>()
            val playerViewModel: VideoPlayerViewModel = viewModel(
                key = "player_${route.anilistId}_${route.episode}_${route.audioType}",
                factory = VideoPlayerViewModel.provideFactory(
                    container = appContainer,
                    anilistId = route.anilistId,
                    episode = route.episode,
                    audioType = route.audioType,
                    title = route.title,
                    totalEpisodes = route.totalEpisodes
                )
            )
            VideoPlayerScreen(
                viewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ScreenRoute.Notifications> {
            NotificationsScreen(
                releaseAnimeList = homeState.dashboard.newEpisodeReleases.ifEmpty { homeState.dashboard.trendingAnime },
                onBack = { navController.popBackStack() },
                onAnimeClick = { anime -> navigateToAnime(anime) }
            )
        }

        composable<ScreenRoute.Downloads> {
            DownloadsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<ScreenRoute.HelpCenter> {
            HelpCenterScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<ScreenRoute.PrivacyPolicy> {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun AniScrollBottomBar(
    currentTab: MainBottomTab,
    onSelectTab: (MainBottomTab) -> Unit
) {
    NavigationBar(
        containerColor = AniSecondaryBackground.copy(alpha = 0.97f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_navigation_bar")
    ) {
        MainBottomTab.entries.forEach { tab ->
            val selected = tab == currentTab
            val icon = when (tab) {
                MainBottomTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                MainBottomTab.SCHEDULE -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
                MainBottomTab.MY_LIST -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
                MainBottomTab.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
                MainBottomTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag(tab.testTag),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AniGreen,
                    selectedTextColor = AniGreen,
                    unselectedIconColor = AniTextSecondary,
                    unselectedTextColor = AniTextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
