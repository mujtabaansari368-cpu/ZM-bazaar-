package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorSurfaceVariantLight = Color(0xFFEDF0F3)
private val ColorSurfaceVariantDark = Color(0xFF232A31)
private val ColorMutedDark = Color(0xFFA0AAB4)

private val LightColorScheme = lightColorScheme(
    primary = ZmGreen,
    onPrimary = ZmWhite,
    primaryContainer = ZmGreenLight,
    onPrimaryContainer = ZmGreenDark,
    secondary = ZmRed,
    onSecondary = ZmWhite,
    secondaryContainer = ZmRedLight,
    onSecondaryContainer = ZmRedDark,
    tertiary = ZmBlack,
    onTertiary = ZmWhite,
    background = ZmSurfaceLight,
    onBackground = ZmCharcoal,
    surface = ZmCardLight,
    onSurface = ZmCharcoal,
    surfaceVariant = ColorSurfaceVariantLight,
    onSurfaceVariant = ZmMutedText,
    outline = ZmBorderLight,
    error = ZmRed,
    onError = ZmWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = ZmGreen,
    onPrimary = ZmWhite,
    primaryContainer = ZmGreenDark,
    onPrimaryContainer = ZmWhite,
    secondary = ZmRed,
    onSecondary = ZmWhite,
    secondaryContainer = ZmRedDark,
    onSecondaryContainer = ZmWhite,
    tertiary = ZmWhite,
    onTertiary = ZmBlack,
    background = ZmSurfaceDark,
    onBackground = ZmWhite,
    surface = ZmCardDark,
    onSurface = ZmWhite,
    surfaceVariant = ColorSurfaceVariantDark,
    onSurfaceVariant = ColorMutedDark,
    outline = ZmBorderDark,
    error = ZmRed,
    onError = ZmWhite
)

@Composable
fun ZmBazaarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZmTypography,
        content = content
    )
}
