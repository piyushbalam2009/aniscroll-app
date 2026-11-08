package com.example.playback.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.example.AniScrollApp
import com.example.R

@OptIn(UnstableApi::class)
class AniScrollHlsDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    Media3DownloadStack.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.nav_downloads,
    0
) {
    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 4102
    }

    override fun getDownloadManager(): DownloadManager {
        val app = applicationContext as AniScrollApp
        return app.container.media3DownloadStack.downloadManager
    }

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val app = applicationContext as AniScrollApp
        return app.container.media3DownloadStack.downloadNotificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            "Downloading anime HLS segments…",
            downloads,
            notMetRequirements
        )
    }
}
