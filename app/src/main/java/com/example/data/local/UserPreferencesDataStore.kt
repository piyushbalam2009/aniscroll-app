package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UserPreferencesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aniscroll_preferences")

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_SUBTITLE_LANG = stringPreferencesKey("default_subtitle_lang")
        val DEFAULT_AUDIO_TYPE = stringPreferencesKey("default_audio_type")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val REMEMBER_POSITION = booleanPreferencesKey("remember_position")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("subtitle_bg_opacity")
        val SELECTED_INTERESTS = stringSetPreferencesKey("selected_interests")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIF_GENERAL = booleanPreferencesKey("notif_general")
        val NOTIF_AIRING = booleanPreferencesKey("notif_airing")
        val NOTIF_NEW_EPISODE = booleanPreferencesKey("notif_new_episode")
        val NOTIF_APP_UPDATES = booleanPreferencesKey("notif_app_updates")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        UserPreferencesState(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: "English (US)",
            defaultSubtitleLang = prefs[Keys.DEFAULT_SUBTITLE_LANG] ?: "English",
            defaultAudioType = prefs[Keys.DEFAULT_AUDIO_TYPE] ?: "sub",
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            defaultVideoQuality = prefs[Keys.DEFAULT_VIDEO_QUALITY] ?: "Auto (1080p)",
            autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            rememberPlaybackPosition = prefs[Keys.REMEMBER_POSITION] ?: true,
            subtitleFontSizeSp = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18f,
            subtitleColorHex = prefs[Keys.SUBTITLE_COLOR] ?: "#FFFFFF",
            subtitleBackgroundOpacity = prefs[Keys.SUBTITLE_BG_OPACITY] ?: 0.65f,
            selectedInterests = prefs[Keys.SELECTED_INTERESTS] ?: emptySet(),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            generalNotifications = prefs[Keys.NOTIF_GENERAL] ?: true,
            airingReminders = prefs[Keys.NOTIF_AIRING] ?: true,
            newEpisodeReminders = prefs[Keys.NOTIF_NEW_EPISODE] ?: true,
            appUpdateNotifications = prefs[Keys.NOTIF_APP_UPDATES] ?: true
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language }
    }

    suspend fun setDefaultSubtitleLang(lang: String) {
        context.dataStore.edit { it[Keys.DEFAULT_SUBTITLE_LANG] = lang }
    }

    suspend fun setDefaultAudioType(audioType: String) {
        context.dataStore.edit { it[Keys.DEFAULT_AUDIO_TYPE] = audioType }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_PLAYBACK_SPEED] = speed }
    }

    suspend fun setDefaultVideoQuality(quality: String) {
        context.dataStore.edit { it[Keys.DEFAULT_VIDEO_QUALITY] = quality }
    }

    suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setRememberPlaybackPosition(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_POSITION] = enabled }
    }

    suspend fun setSubtitleAppearance(fontSizeSp: Float, colorHex: String, bgOpacity: Float) {
        context.dataStore.edit {
            it[Keys.SUBTITLE_FONT_SIZE] = fontSizeSp
            it[Keys.SUBTITLE_COLOR] = colorHex
            it[Keys.SUBTITLE_BG_OPACITY] = bgOpacity
        }
    }

    suspend fun completeOnboarding(interests: Set<String>) {
        context.dataStore.edit {
            it[Keys.SELECTED_INTERESTS] = interests
            it[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setSelectedInterests(interests: Set<String>) {
        context.dataStore.edit { it[Keys.SELECTED_INTERESTS] = interests }
    }

    suspend fun setNotificationPreferences(
        general: Boolean? = null,
        airing: Boolean? = null,
        newEpisode: Boolean? = null,
        appUpdates: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            general?.let { prefs[Keys.NOTIF_GENERAL] = it }
            airing?.let { prefs[Keys.NOTIF_AIRING] = it }
            newEpisode?.let { prefs[Keys.NOTIF_NEW_EPISODE] = it }
            appUpdates?.let { prefs[Keys.NOTIF_APP_UPDATES] = it }
        }
    }
}
