package com.example.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Anime
import com.example.ui.theme.AniBackground
import com.example.ui.theme.AniCardBackground
import com.example.ui.theme.AniGreen
import com.example.ui.theme.AniSecondaryBackground
import com.example.ui.theme.AniTextPrimary
import com.example.ui.theme.AniTextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AniScrollLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = AniGreen
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.22f

        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.80f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.78f, h * 0.80f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawCircle(
            color = color,
            radius = w * 0.12f,
            center = Offset(w * 0.50f, h * 0.76f)
        )
    }
}

@Composable
fun SpinningDotsLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = AniGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_loader")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .rotate(angle)
    ) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val dotCount = 8
        for (i in 0 until dotCount) {
            val theta = Math.toRadians((i * 360.0 / dotCount))
            val distance = radius * 0.72f
            val x = center.x + (distance * cos(theta)).toFloat()
            val y = center.y + (distance * sin(theta)).toFloat()
            val dotFraction = (i + 1).toFloat() / dotCount.toFloat()
            drawCircle(
                color = color.copy(alpha = 0.25f + 0.75f * dotFraction),
                radius = radius * (0.10f + 0.12f * dotFraction),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun GreenPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AniGreen,
            contentColor = Color.White,
            disabledContainerColor = AniGreen.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GreenOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = AniGreen,
    contentColor: Color = AniGreen,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SelectablePillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val bgColor = if (selected) AniGreen else Color.Transparent
    val textColor = if (selected) Color.White else AniGreen
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.5.dp, AniGreen, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAllClick != null) {
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                color = AniGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rankNumber: Int? = null,
    episodeLabel: String? = null,
    showTitleBelow: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("anime_card_${anime.anilistId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.71f)
                .clip(RoundedCornerShape(14.dp))
                .background(AniCardBackground)
        ) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = anime.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom subtle dark gradient for rank or episode label legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            // Score badge top-left matching reference screenshot
            Surface(
                color = AniGreen,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = anime.formattedScore,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            if (rankNumber != null) {
                Text(
                    text = rankNumber.toString(),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 6.dp)
                )
            } else if (episodeLabel != null) {
                Text(
                    text = episodeLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            } else if (!showTitleBelow) {
                Text(
                    text = anime.displayTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        if (showTitleBelow) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = anime.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopHitAnimeRowCard(
    anime: Anime,
    rank: Int,
    inWatchlist: Boolean,
    onCardClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("top_hit_item_${anime.anilistId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimePosterCard(
            anime = anime,
            onClick = onCardClick,
            rankNumber = rank,
            modifier = Modifier.width(126.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = anime.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${anime.releaseYear ?: "2024"} | ${anime.regionDisplay}",
                style = MaterialTheme.typography.bodySmall,
                color = AniTextSecondary
            )
            if (anime.genres.isNotEmpty()) {
                Text(
                    text = "Genre: ${anime.genres.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AniTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (inWatchlist) {
                GreenOutlinedButton(
                    text = "My List",
                    icon = Icons.Default.Check,
                    onClick = onToggleWatchlist,
                    testTag = "top_hit_mylist_${anime.anilistId}"
                )
            } else {
                GreenPrimaryButton(
                    text = "My List",
                    icon = Icons.Default.Add,
                    onClick = onToggleWatchlist,
                    testTag = "top_hit_mylist_${anime.anilistId}"
                )
            }
        }
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(AniCardBackground.copy(alpha = alpha))
    )
}

enum class EmptyIllustrationType {
    EMPTY_LIST,
    EMPTY_SCHEDULE,
    NOT_FOUND_404,
    EMPTY_DOWNLOAD,
    EMPTY_HISTORY
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    illustrationType: EmptyIllustrationType,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val w = size.width
            val h = size.height
            when (illustrationType) {
                EmptyIllustrationType.EMPTY_LIST, EmptyIllustrationType.EMPTY_HISTORY -> {
                    // Two dark clipboards with bright green clips (matches Screenshot 44)
                    drawRoundRect(
                        color = Color(0xFF1A1E29),
                        topLeft = Offset(w * 0.16f, h * 0.18f),
                        size = Size(w * 0.46f, h * 0.62f),
                        cornerRadius = CornerRadius(20f, 20f)
                    )
                    drawRoundRect(
                        color = AniGreen,
                        topLeft = Offset(w * 0.27f, h * 0.13f),
                        size = Size(w * 0.24f, h * 0.09f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color(0xFF252A38),
                        topLeft = Offset(w * 0.36f, h * 0.24f),
                        size = Size(w * 0.48f, h * 0.62f),
                        cornerRadius = CornerRadius(20f, 20f)
                    )
                    drawRoundRect(
                        color = AniGreen,
                        topLeft = Offset(w * 0.48f, h * 0.19f),
                        size = Size(w * 0.24f, h * 0.09f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }

                EmptyIllustrationType.EMPTY_SCHEDULE -> {
                    // Large sad green circle emoji with dark background blob (matches Screenshot 30)
                    drawCircle(
                        color = Color(0xFF10281E),
                        radius = w * 0.42f,
                        center = Offset(w * 0.5f, h * 0.48f)
                    )
                    drawCircle(
                        color = AniGreen,
                        radius = w * 0.31f,
                        center = Offset(w * 0.48f, h * 0.52f)
                    )
                    // Sad mouth
                    drawArc(
                        color = Color(0xFF232733),
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = true,
                        topLeft = Offset(w * 0.33f, h * 0.52f),
                        size = Size(w * 0.30f, h * 0.22f)
                    )
                    // Closed eyes
                    drawArc(
                        color = Color(0xFF232733),
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.32f, h * 0.38f),
                        size = Size(w * 0.11f, h * 0.06f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF232733),
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.52f, h * 0.38f),
                        size = Size(w * 0.11f, h * 0.06f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                }

                EmptyIllustrationType.NOT_FOUND_404 -> {
                    // Mountains, green moon, and 404 motif (matches Screenshot 26)
                    drawCircle(
                        color = AniGreen,
                        radius = w * 0.08f,
                        center = Offset(w * 0.68f, h * 0.22f)
                    )
                    val mtn = Path().apply {
                        moveTo(w * 0.1f, h * 0.78f)
                        lineTo(w * 0.45f, h * 0.28f)
                        lineTo(w * 0.85f, h * 0.78f)
                        close()
                    }
                    drawPath(mtn, color = Color(0xFF1A1E28))
                    drawCircle(
                        color = Color(0xFF8C92A4),
                        radius = w * 0.15f,
                        center = Offset(w * 0.5f, h * 0.62f),
                        style = Stroke(width = 8f)
                    )
                    drawCircle(
                        color = AniGreen,
                        radius = w * 0.05f,
                        center = Offset(w * 0.5f, h * 0.70f)
                    )
                }

                EmptyIllustrationType.EMPTY_DOWNLOAD -> {
                    // Green tree + play badge (matches Screenshot 46)
                    drawCircle(
                        color = AniGreen,
                        radius = w * 0.22f,
                        center = Offset(w * 0.34f, h * 0.34f)
                    )
                    drawLine(
                        color = Color(0xFF34394A),
                        start = Offset(w * 0.34f, h * 0.34f),
                        end = Offset(w * 0.34f, h * 0.82f),
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = Color(0xFF2B2F42),
                        radius = w * 0.17f,
                        center = Offset(w * 0.68f, h * 0.46f)
                    )
                    val playPath = Path().apply {
                        moveTo(w * 0.64f, h * 0.39f)
                        lineTo(w * 0.75f, h * 0.46f)
                        lineTo(w * 0.64f, h * 0.53f)
                        close()
                    }
                    drawPath(playPath, color = AniGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AniGreen,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AniTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            GreenPrimaryButton(
                text = actionButtonText,
                onClick = onActionClick
            )
        }
    }
}
