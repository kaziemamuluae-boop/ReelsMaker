package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PremiumRed,
    onPrimary = TextPrimary,
    primaryContainer = DarkCrimson,
    onPrimaryContainer = TextPrimary,
    secondary = CrimsonRed,
    onSecondary = TextPrimary,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = DarkGraySurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = TextSecondary,
    outline = LightBorder
)

@Composable
fun ReelsMakerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

