package com.streamflex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// StreamFlex is always dark — no light mode, no dynamic color
private val StreamFlexColorScheme = darkColorScheme(
    primary            = SFAccent,
    onPrimary          = SFTextOnAccent,
    primaryContainer   = SFAccentDim,
    onPrimaryContainer = SFTextPrimary,

    secondary          = SFAccentAlt,
    onSecondary        = SFTextOnAccent,

    background         = SFBgPrimary,
    onBackground       = SFTextPrimary,

    surface            = SFBgCard,
    onSurface          = SFTextPrimary,
    surfaceVariant     = SFBgSurface,
    onSurfaceVariant   = SFTextSecondary,

    surfaceContainer       = SFBgCard,
    surfaceContainerLow    = SFBgPrimary,
    surfaceContainerHigh   = SFBgSurface,
    surfaceContainerHighest= SFBgElevated,

    outline            = SFOutline,
    outlineVariant     = SFDivider,

    error              = SFError,
    onError            = SFTextOnAccent,
)


private val StreamFlexLightColorScheme = lightColorScheme(
    primary            = SFAccent,
    onPrimary          = SFTextOnAccent,
    primaryContainer   = SFAccentGlow,
    onPrimaryContainer = SFTextPrimary,

    secondary          = SFAccentAlt,
    onSecondary        = SFTextOnAccent,

    background         = Color(0xFFF8F9FA),
    onBackground       = Color(0xFF1E2022),

    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF1E2022),
    surfaceVariant     = Color(0xFFF1F3F5),
    onSurfaceVariant   = Color(0xFF495057),

    surfaceContainer       = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFF8F9FA),
    surfaceContainerHigh   = Color(0xFFF1F3F5),
    surfaceContainerHighest= Color(0xFFE9ECEF),

    outline            = Color(0xFFDEE2E6),
    outlineVariant     = Color(0x1A000000),

    error              = SFError,
    onError            = SFTextOnAccent,
)

@Composable
fun StreamFlexTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StreamFlexColorScheme else StreamFlexLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SFTypography,
        content     = content
    )
}