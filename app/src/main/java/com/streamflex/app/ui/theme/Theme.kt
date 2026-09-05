package com.streamflex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. DEFAULT DARK (Sky Stream Dark)
private val SkyDarkColorScheme = darkColorScheme(
    primary            = Color(0xFF0088FF), // Vibrant Blue
    onPrimary          = Color.White,
    background         = Color(0xFF121212), // Sleek Dark Gray
    onBackground       = Color(0xFFFFFFFF), // High contrast white
    surface            = Color(0xFF1A1A1A), // Elevated surface
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF242424), // Cards and Chips
    onSurfaceVariant   = Color(0xFFAAAAAA), // Secondary text
    outline            = Color(0xFF333333),
    outlineVariant     = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF242424)
)

// 2. DEFAULT LIGHT (Sky Stream Light) - Sweet cream with shine red
private val SkyLightColorScheme = lightColorScheme(
    primary            = Color(0xFFFF3B30), // Shine Red
    onPrimary          = Color.White,
    background         = Color(0xFFFFF8F0), // Sweet Cream
    onBackground       = Color(0xFF2D2422), // Dark Brown/Black
    surface            = Color(0xFFFFFFFF), // Pure White for cards
    onSurface          = Color(0xFF2D2422),
    surfaceVariant     = Color(0xFFFFEAD6), // Creamy accent
    onSurfaceVariant   = Color(0xFF7A6458), // Brownish secondary text
    outline            = Color(0xFFF3D5C0),
    outlineVariant     = Color(0xFFF3D5C0),
    surfaceContainerHighest = Color(0xFFFFEAD6)
)

// 3. NETFLIX MODE (Premium Dark)
private val NetflixColorScheme = darkColorScheme(
    primary            = Color(0xFFE50914), // Signature Netflix Red
    onPrimary          = Color.White,
    background         = Color(0xFF000000), // Pure Netflix Black
    onBackground       = Color(0xFFFFFFFF),
    surface            = Color(0xFF141414), // Netflix dark grey surface
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF1F1F1F), // Cards
    onSurfaceVariant   = Color(0xFFB3B3B3), // Netflix subtitle gray
    outline            = Color(0xFF333333), // Subtle outlines
    outlineVariant     = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF1F1F1F)
)

// 4. PRIME MODE (Enhanced Contrast)
private val PrimeColorScheme = darkColorScheme(
    primary            = Color(0xFF00A8E1), // Prime Cyan
    onPrimary          = Color.White,
    background         = Color(0xFF0F171E), // Prime Navy Background
    onBackground       = Color(0xFFFFFFFF),
    surface            = Color(0xFF19222C), // Prime Surface
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF232F3D), // Prime header/cards
    onSurfaceVariant   = Color(0xFF798997), // Prime gray text
    outline            = Color(0xFF344654),
    outlineVariant     = Color(0xFF344654),
    surfaceContainerHighest = Color(0xFF232F3D)
)

// 5. CINEMATIC OLED (StreamFlex Premium)
private val StreamFlexColorScheme = darkColorScheme(
    primary            = Color(0xFF00F0FF), // Neon Cyan (Very high contrast)
    onPrimary          = Color.Black,
    background         = Color(0xFF000000), // OLED Pitch Black
    onBackground       = Color(0xFFFFFFFF), // Pure White
    surface            = Color(0xFF0A0A0A), // Barely visible surface
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF141414), // Slightly elevated cards
    onSurfaceVariant   = Color(0xFF9BA0A4), // Muted text
    outline            = Color(0xFF222222), // Minimal outlines
    outlineVariant     = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF141414)
)

@Composable
fun StreamFlexTheme(
    appTheme: String = "SKY_DARK",
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        "SKY_LIGHT" -> SkyLightColorScheme
        "SKY_DARK" -> SkyDarkColorScheme
        "NETFLIX" -> NetflixColorScheme
        "PRIME" -> PrimeColorScheme
        "STREAMFLEX" -> StreamFlexColorScheme
        else -> SkyDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SFTypography,
        content     = content
    )
}
