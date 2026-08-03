package com.streamflex.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── StreamFlex Design Tokens ──────────────────────────────────────────────────
// Mirrors globals.css CSS variables for Android ↔ Web visual parity

// Backgrounds
val SFBgPrimary       = Color(0xFF0E0E11)  // Main canvas (near black)
val SFBgCard          = Color(0xFF16161A)  // Card / poster background
val SFBgSurface       = Color(0xFF1C1C22)  // Bottom sheet / modal surface
val SFBgElevated      = Color(0xFF26262E)  // Search bar / chips / elevated
val SFBgNavBar        = Color(0xFF0E0E11)  // Bottom nav bar
val SFBgTopBar        = Color(0x99000000)  // Scrolled top bar (60% black)

// Brand Accent
val SFAccent          = Color(0xFF3D50FA)  // Primary electric blue
val SFAccentDim       = Color(0xFF2A3AB5)  // Pressed / darker accent
val SFAccentAlt       = Color(0xFFF53B66)  // Sub / Red accent
val SFAccentGlow      = Color(0x263D50FA)  // Glow/shimmer (15% accent)

// Text
val SFTextPrimary     = Color(0xFFE9EAEE)  // Main readable text
val SFTextSecondary   = Color(0xFF9BA0A4)  // Muted / supporting text
val SFTextDisabled    = Color(0xFF5A5D63)  // Disabled / placeholder text
val SFTextOnAccent    = Color(0xFFFFFFFF)  // Text on accent-colored buttons

// Semantic / Badge
val SFDubBg           = Color(0xFF3B65F5)  // DUB badge background
val SFSubBg           = Color(0xFFF53B66)  // SUB badge background
val SFRatingBg        = Color(0xFFFF9800)  // IMDB-style rating chip
val SFRatingText      = Color(0xFF1A1000)  // Rating text
val SFHDTag           = Color(0xFF00C896)  // HD / 4K quality tag

// Divider / Outline
val SFOutline         = Color(0xFF2A2A32)  // Subtle border lines
val SFDivider         = Color(0x1AFFFFFF)  // 10% white divider

// Overlays
val SFPosterOverlay   = Color(0xB3000000)  // 70% black on poster hover
val SFHeroGradStart   = Color(0x00000000)  // Transparent (gradient top)
val SFHeroGradEnd     = Color(0xFF0E0E11)  // Full bg color (gradient end)

// Player
val SFPlayerBg        = Color(0xFF000000)  // Pure black player background
val SFProgressActive  = Color(0xFF3D50FA)  // Seek bar filled
val SFProgressBg      = Color(0x33FFFFFF)  // Seek bar track

// State
val SFSuccess         = Color(0xFF48E484)  // Provider online indicator
val SFWarning         = Color(0xFFFF9800)  // Provider degraded
val SFError           = Color(0xFFEA596E)  // Provider offline / error

// Legacy aliases (kept so existing references still compile)
val Purple80          = SFAccent
val PurpleGrey80      = SFBgSurface
val Pink80            = SFAccentAlt
val Purple40          = SFAccentDim
val PurpleGrey40      = SFBgElevated
val Pink40            = SFSubBg