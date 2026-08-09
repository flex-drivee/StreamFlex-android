package com.streamflex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. SKY DARK
private val SkyDarkColorScheme = darkColorScheme(
    primary            = Color(0xFF448AFF),
    onPrimary          = Color.White,
    background         = Color(0xFF000000),
    onBackground       = Color(0xFFF9FAFB),
    surface            = Color(0xFF000000),
    onSurface          = Color(0xFFF9FAFB),
    surfaceVariant     = Color(0xFF18181F),
    onSurfaceVariant   = Color(0xFF9CA3AF),
    outline            = Color(0xFF22222E),
    outlineVariant     = Color(0xFF22222E),
    surfaceContainerHighest = Color(0xFF18181F)
)

// 2. SKY LIGHT
private val SkyLightColorScheme = lightColorScheme(
    primary            = Color(0xFFC63523),
    onPrimary          = Color.White,
    background         = Color(0xFFF5F1EC),
    onBackground       = Color(0xFF2C2521),
    surface            = Color(0xFFFAF8F5),
    onSurface          = Color(0xFF2C2521),
    surfaceVariant     = Color(0xFFE8E2D8),
    onSurfaceVariant   = Color(0xFF5C5C5C),
    outline            = Color(0xFFD9C9AE),
    outlineVariant     = Color(0xFFD9C9AE),
    surfaceContainerHighest = Color(0xFFE8E2D8)
)

// 3. NETFLIX
private val NetflixColorScheme = darkColorScheme(
    primary            = Color(0xFFE50914),
    onPrimary          = Color.White,
    background         = Color(0xFF000000),
    onBackground       = Color.White,
    surface            = Color(0xFF141414),
    onSurface          = Color.White,
    surfaceVariant     = Color(0xFF2B2B2B),
    onSurfaceVariant   = Color(0xFF808080),
    outline            = Color(0xFF404040),
    outlineVariant     = Color(0xFF404040),
    surfaceContainerHighest = Color(0xFF2B2B2B)
)

// 4. AMAZON PRIME
private val PrimeColorScheme = darkColorScheme(
    primary            = Color(0xFF00A8E1),
    onPrimary          = Color.White,
    background         = Color(0xFF0F171E),
    onBackground       = Color.White,
    surface            = Color(0xFF1B2530),
    onSurface          = Color.White,
    surfaceVariant     = Color(0xFF252F3A),
    onSurfaceVariant   = Color(0xFF8197A4),
    outline            = Color(0xFF334351),
    outlineVariant     = Color(0xFF334351),
    surfaceContainerHighest = Color(0xFF252F3A)
)

// 5. STREAMFLEX (PREMIUM)
private val StreamFlexColorScheme = darkColorScheme(
    primary            = Color(0xFF3D50FA),
    onPrimary          = Color.White,
    background         = Color(0xFF0E0E11),
    onBackground       = Color(0xFFE9EAEE),
    surface            = Color(0xFF16161A),
    onSurface          = Color(0xFFE9EAEE),
    surfaceVariant     = Color(0xFF1C1C22),
    onSurfaceVariant   = Color(0xFF9BA0A4),
    outline            = Color(0xFF2A2A32),
    outlineVariant     = Color(0x1AFFFFFF),
    surfaceContainerHighest = Color(0xFF26262E)
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