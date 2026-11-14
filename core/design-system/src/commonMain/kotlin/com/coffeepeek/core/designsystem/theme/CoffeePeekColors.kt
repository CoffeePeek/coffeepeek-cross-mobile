package com.coffeepeek.core.designsystem.theme

import androidx.compose.ui.graphics.Color

object CoffeePeekColors {

    // ── Brand ─────────────────────────────────────────────────────────────────
    val Primary        = Color(0xFFFACC15)
    val SwitchCheckedThumbLight = Color(0xFF684900)
    val SwitchCheckedTrackDark = Color(0xFF9A7A28)
    val SwitchCheckedThumbDark = Color(0xFFFFF7D6)

    val GoldWarm       = Color(0xFFD4A84B)
    val GoldWarmSoft   = Color(0xFFF8F1DD)

    // ── Semantic ──────────────────────────────────────────────────────────────
    val Success        = Color(0xFF22C55E)
    val SuccessTextLight = Color(0xFF166534)
    val ErrorTextLight = Color(0xFFB91C1C)
    val ErrorTextDark = Color(0xFFFCA5A5)
    // Positive affinity has its own semantic role, using brand gold rather than error red.
    val Like           = Primary
    val Overlay = Color(0x80000000)
    val PhotoBadgeOverlay = Color.Black.copy(alpha = 0.68f)

    // ── Light palette ─────────────────────────────────────────────────────────
    val LightBackground    = Color(0xFFFAFAF9)
    val LightSurface       = Color(0xFFFFFFFF)
    val LightSurfaceAlt    = Color(0xFFF9F8F6)
    val LightBadge         = Color(0xFFF3F4F6)
    val LightTextPrimary   = Color(0xFF1C1917)
    val LightTextSecondary = Color(0xFF6B655F)
    val LightTextOnPrimary = Color(0xFF1A1412)
    val LightBorder        = Color(0xFFE7E5E4)
    val LightBorderSubtle  = Color(0xFFE5E1DA)

    // ── Dark palette ──────────────────────────────────────────────────────────
    val DarkBackground    = Color(0xFF1A1412)
    val DarkSurface       = Color(0xFF2D241F)
    val DarkTextPrimary   = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFFA39E93)
    val DarkTextOnPrimary = Color(0xFF1A1412)
    val DarkBorder        = Color(0xFF3D2F28)
    val DarkBorderHover   = Color(0xFF4A3D35)
}
