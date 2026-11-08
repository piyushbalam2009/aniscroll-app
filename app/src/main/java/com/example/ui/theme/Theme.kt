package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AniGreen,
    onPrimary = AniTextPrimary,
    primaryContainer = AniGreenSurface,
    onPrimaryContainer = AniGreen,
    secondary = AniGreenLight,
    onSecondary = AniBackground,
    secondaryContainer = AniCardBackground,
    onSecondaryContainer = AniTextPrimary,
    tertiary = AniGreen,
    background = AniBackground,
    onBackground = AniTextPrimary,
    surface = AniSecondaryBackground,
    onSurface = AniTextPrimary,
    surfaceVariant = AniCardBackground,
    onSurfaceVariant = AniTextSecondary,
    outline = AniBorder,
    error = AniError,
    onError = AniTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = AniGreen,
    onPrimary = AniTextPrimary,
    primaryContainer = Color(0xFFD8F8E3),
    onPrimaryContainer = AniGreenDark,
    secondary = AniGreenDark,
    onSecondary = AniTextPrimary,
    secondaryContainer = AniLightSecondaryBackground,
    onSecondaryContainer = AniLightTextPrimary,
    background = AniLightBackground,
    onBackground = AniLightTextPrimary,
    surface = AniLightSecondaryBackground,
    onSurface = AniLightTextPrimary,
    surfaceVariant = AniLightCardBackground,
    onSurfaceVariant = AniLightTextSecondary,
    outline = Color(0xFFD5DCE6),
    error = AniError,
    onError = AniTextPrimary
)

val AniShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AniScrollTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AniShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AniScrollTheme(darkTheme = darkTheme, content = content)
}
