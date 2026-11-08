package com.example.core.common

import kotlinx.coroutines.delay
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed interface AppResult<out T> {
    data object Loading : AppResult<Nothing>
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : AppResult<Nothing>
}

suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 600L,
    maxDelayMs: Long = 4000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxRetries - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val isTransient = e is IOException || (e.message?.contains("429") == true) || (e.message?.contains("50") == true)
            if (!isTransient && attempt > 0) throw e
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
    }
    return block()
}

object TimeUtils {
    fun formatAiringTime(epochSeconds: Long): String {
        val date = Date(epochSeconds * 1000L)
        val tz = TimeZone.getDefault()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = tz
        val isIst = tz.id == "Asia/Kolkata" || tz.id == "Asia/Calcutta" || tz.rawOffset == 19800000
        val suffix = if (isIst) " IST" else ""
        return "${sdf.format(date)}$suffix"
    }

    fun formatCountdown(secondsUntilAiring: Long): String {
        if (secondsUntilAiring <= 0) return "Aired"
        val days = secondsUntilAiring / 86400
        val hours = (secondsUntilAiring % 86400) / 3600
        val mins = (secondsUntilAiring % 3600) / 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${mins}m")
        }.trim()
    }

    fun formatDurationMs(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatRelativeDate(timestampMs: Long): String {
        val diff = System.currentTimeMillis() - timestampMs
        val mins = diff / 60000
        val hours = mins / 60
        val days = hours / 24
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(timestampMs))
        }
    }

    fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun getCurrentAnimeSeason(): String {
        val month = Calendar.getInstance().get(Calendar.MONTH) // 0..11
        return when (month) {
            in 0..2 -> "WINTER"
            in 3..5 -> "SPRING"
            in 6..8 -> "SUMMER"
            else -> "FALL"
        }
    }

    fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&rsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
            .replace("&ndash;", "-")
            .replace("&mdash;", "—")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
