package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PlayStationColorScheme = darkColorScheme(
    primary = TrophyGold,
    onPrimary = CosmicBackground,
    primaryContainer = TrophyGoldDark,
    onPrimaryContainer = TextPrimary,
    secondary = PSBlueAccent,
    onSecondary = TextPrimary,
    secondaryContainer = PSBlue,
    onSecondaryContainer = TextPrimary,
    tertiary = PSBlueLight,
    onTertiary = CosmicBackground,
    background = CosmicBackground,
    onBackground = TextPrimary,
    surface = CosmicSurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = CosmicSurface,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PlayStationColorScheme,
        typography = Typography,
        content = content
    )
}
