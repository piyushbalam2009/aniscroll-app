package com.example.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.AppResult
import com.example.core.common.TimeUtils
import com.example.core.designsystem.AniScrollLogo
import com.example.core.designsystem.EmptyIllustrationType
import com.example.core.designsystem.EmptyStateView
import com.example.core.designsystem.GreenOutlinedButton
import com.example.core.designsystem.GreenPrimaryButton
import com.example.core.designsystem.SpinningDotsLoader
import com.example.domain.model.AiringScheduleItem
import com.example.domain.model.Anime
import com.example.ui.theme.AniBorder
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayPillModel(
    val offset: Int,
    val dayName: String,
    val dayOfMonth: String
)

@Composable
fun ReleaseCalendarScreen(
    selectedDayOffset: Int,
    scheduleState: AppResult<List<AiringScheduleItem>>,
    watchlistIds: Set<Int>,
    scheduledReminderIds: Set<Int>,
    onSelectDayOffset: (Int) -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onToggleWatchlist: (Anime) -> Unit,
    onToggleReminder: (AiringScheduleItem) -> Unit,
    onRetry: () -> Unit
) {
    val days = remember {
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val numFormat = SimpleDateFormat("dd", Locale.US)
        (-1..5).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            DayPillModel(
                offset = offset,
                dayName = dayFormat.format(cal.time),
                dayOfMonth = numFormat.format(cal.time)
            )
        }
    }

    val currentTimeLabel = remember {
        TimeUtils.formatAiringTime(System.currentTimeMillis() / 1000L)
    }
    val nowEpochSec = System.currentTimeMillis() / 1000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("release_calendar_screen")
    ) {
        // Top Header matching Screenshot 30 & 31
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AniScrollLogo(size = 30.dp)
            Text(
                text = stringResource(R.string.release_calendar),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Horizontal Day Selector matching Screenshot 30 & 31
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(days, key = { it.offset }) { day ->
                val isSelected = day.offset == selectedDayOffset
                val bgColor = if (isSelected) AniGreen else Color.Transparent
                val borderColor = if (isSelected) AniGreen else AniBorder
                val textColor = if (isSelected) Color.White else AniTextSecondary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(54.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(50))
                        .clickable { onSelectDayOffset(day.offset) }
                        .testTag("calendar_day_${day.offset}")
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day.dayOfMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        when (scheduleState) {
            is AppResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SpinningDotsLoader()
                }
            }

            is AppResult.Error -> {
                EmptyStateView(
                    title = stringResource(R.string.no_release_schedule_title),
                    subtitle = scheduleState.message,
                    illustrationType = EmptyIllustrationType.EMPTY_SCHEDULE,
                    actionButtonText = "Retry",
                    onActionClick = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is AppResult.Success -> {
                if (scheduleState.data.isEmpty()) {
                    EmptyStateView(
                        title = stringResource(R.string.no_release_schedule_title),
                        subtitle = stringResource(R.string.no_release_schedule_subtitle),
                        illustrationType = EmptyIllustrationType.EMPTY_SCHEDULE,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val itemsList = scheduleState.data
                    val currentTimeMarkerIndex = remember(itemsList, selectedDayOffset) {
                        if (selectedDayOffset != 0) -1
                        else itemsList.indexOfFirst { it.airingAtEpoch > nowEpochSec }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            itemsList,
                            key = { _, item -> "sched_${item.id}_${item.anilistId}_${item.episode}" }
                        ) { index, item ->
                            if (index == currentTimeMarkerIndex) {
                                CurrentTimeMarkerRow(currentTimeLabel = currentTimeLabel)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            ScheduleTimelineCard(
                                item = item,
                                inWatchlist = item.anilistId in watchlistIds,
                                reminderSet = item.id in scheduledReminderIds,
                                onClick = { item.anime?.let(onAnimeClick) },
                                onToggleWatchlist = { item.anime?.let(onToggleWatchlist) },
                                onToggleReminder = { onToggleReminder(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeMarkerRow(currentTimeLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            color = AniGreen,
            thickness = 2.dp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Current Time - $currentTimeLabel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(
            color = AniGreen,
            thickness = 2.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScheduleTimelineCard(
    item: AiringScheduleItem,
    inWatchlist: Boolean,
    reminderSet: Boolean,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onToggleReminder: () -> Unit
) {
    val anime = item.anime
    val timeStr = TimeUtils.formatAiringTime(item.airingAtEpoch)
    val countdownStr = TimeUtils.formatCountdown(item.airingAtEpoch - (System.currentTimeMillis() / 1000L))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = anime != null, onClick = onClick)
            .testTag("schedule_item_${item.anilistId}")
    ) {
        // Time marker row matching Screenshot 31
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AniGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "($countdownStr)",
                    style = MaterialTheme.typography.labelMedium,
                    color = AniGreen
                )
            }

            IconButton(
                onClick = onToggleReminder,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (reminderSet) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                    contentDescription = "Set Reminder",
                    tint = if (reminderSet) AniGreen else AniTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(142.dp)
                    .aspectRatio(1.42f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AniCardBackground)
            ) {
                AsyncImage(
                    model = anime?.coverUrl?.takeIf { it.isNotBlank() } ?: anime?.posterUrl,
                    contentDescription = anime?.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.White.copy(alpha = 0.88f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = anime?.displayTitle ?: "Anime #${item.anilistId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Episodes ${item.episode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AniTextSecondary
                )
                if (inWatchlist) {
                    GreenOutlinedButton(
                        text = "My List",
                        icon = Icons.Default.Check,
                        onClick = onToggleWatchlist
                    )
                } else {
                    GreenPrimaryButton(
                        text = "My List",
                        icon = Icons.Default.Add,
                        onClick = onToggleWatchlist
                    )
                }
            }
        }
    }
}
