package com.streamflex.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Inter font family — matches web (Google Fonts Inter)
// Requires downloading Inter .ttf fonts to app/src/main/res/font/
// Files needed: inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf, inter_extrabold.ttf
// Until fonts are added, uses Default (Roboto) — same visual weight

// Inter font family — matches web (Google Fonts Inter)
// Swap FontFamily.Default below for actual Inter fonts once .ttf files are placed in res/font/:
//   Font(R.font.inter_regular,   FontWeight.Normal)
//   Font(R.font.inter_medium,    FontWeight.Medium)
//   Font(R.font.inter_semibold,  FontWeight.SemiBold)
//   Font(R.font.inter_bold,      FontWeight.Bold)
//   Font(R.font.inter_extrabold, FontWeight.ExtraBold)
val InterFamily = FontFamily.Default

val SFTypography = Typography(
    // Hero title — movie/show name in hero banner
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    // Section headings e.g. "Trending Movies"
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // Card titles, modal titles
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body / overview text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp
    ),
    // Badges, captions, labels
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.4.sp
    ),
)

// Alias so Theme.kt compiles (legacy reference)
val Typography = SFTypography