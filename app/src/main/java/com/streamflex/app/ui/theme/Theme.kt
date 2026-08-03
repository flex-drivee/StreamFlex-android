package com.streamflex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun StreamFlexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StreamFlexColorScheme,
        typography  = SFTypography,
        content     = content
    )
}