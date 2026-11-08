package com.example.feature.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.domain.model.AiringScheduleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class AiringReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "New Anime Episode"
        val episode = inputData.getInt("episode", 1)
        val anilistId = inputData.getInt("anilistId", 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.CHANNEL_AIRING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Episode $episode Airing Now!")
            .setContentText("$title — Episode $episode is now available to stream on AniScroll.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(anilistId * 100 + episode, notification)
        }
        return Result.success()
    }
}

class NotificationScheduler(private val context: Context) {
    companion object {
        const val CHANNEL_AIRING = "aniscroll_airing_reminders"
        const val CHANNEL_GENERAL = "aniscroll_general_updates"
    }

    private val _scheduledIds = MutableStateFlow<Set<Int>>(emptySet())
    val scheduledIds: StateFlow<Set<Int>> = _scheduledIds.asStateFlow()

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val airingChannel = NotificationChannel(
                CHANNEL_AIRING,
                "Episode Airing Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Local reminders when scheduled anime episodes air"
            }
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General & Release Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Local release notifications and app updates"
            }
            manager.createNotificationChannel(airingChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }

    fun toggleEpisodeReminder(item: AiringScheduleItem): Boolean {
        val workName = "airing_reminder_${item.anilistId}_${item.episode}"
        val workManager = WorkManager.getInstance(context)
        val current = _scheduledIds.value

        return if (item.id in current) {
            workManager.cancelUniqueWork(workName)
            _scheduledIds.value = current - item.id
            false
        } else {
            val delaySec = (item.airingAtEpoch - (System.currentTimeMillis() / 1000L)).coerceAtLeast(5L)
            val data = Data.Builder()
                .putInt("anilistId", item.anilistId)
                .putInt("episode", item.episode)
                .putString("title", item.anime?.displayTitle ?: "Anime Episode")
                .build()

            val request = OneTimeWorkRequestBuilder<AiringReminderWorker>()
                .setInitialDelay(delaySec, TimeUnit.SECONDS)
                .setInputData(data)
                .build()

            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
            _scheduledIds.value = current + item.id
            true
        }
    }
}
