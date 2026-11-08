package com.example.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ScreenRoute {
    @Serializable
    data object Splash : ScreenRoute

    @Serializable
    data object Welcome : ScreenRoute

    @Serializable
    data object ChooseInterest : ScreenRoute

    @Serializable
    data object Main : ScreenRoute

    @Serializable
    data object TopHits : ScreenRoute

    @Serializable
    data object NewReleases : ScreenRoute

    @Serializable
    data class Search(val initialQuery: String = "", val openFilter: Boolean = false) : ScreenRoute

    @Serializable
    data object SortFilter : ScreenRoute

    @Serializable
    data class AnimeDetails(val anilistId: Int) : ScreenRoute

    @Serializable
    data class MovieDetails(val anilistId: Int) : ScreenRoute

    @Serializable
    data class VideoPlayer(
        val anilistId: Int,
        val episode: Int,
        val audioType: String = "sub",
        val title: String = "",
        val totalEpisodes: Int = 12
    ) : ScreenRoute

    @Serializable
    data object Notifications : ScreenRoute

    @Serializable
    data object Downloads : ScreenRoute

    @Serializable
    data object HelpCenter : ScreenRoute

    @Serializable
    data object PrivacyPolicy : ScreenRoute
}

enum class MainBottomTab(val label: String, val testTag: String) {
    HOME("Home", "tab_home"),
    SCHEDULE("Release Ca..", "tab_schedule"),
    MY_LIST("My List", "tab_my_list"),
    HISTORY("History", "tab_history"),
    SETTINGS("Settings", "tab_settings")
}
