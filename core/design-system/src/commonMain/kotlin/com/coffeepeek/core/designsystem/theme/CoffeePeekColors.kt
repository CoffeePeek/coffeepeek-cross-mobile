package com.coffeepeek.core.designsystem.theme

import androidx.compose.ui.graphics.Color

object CoffeePeekColors {

    // ── Brand ─────────────────────────────────────────────────────────────────
    val Primary        = Color(0xFFEAB308)
    val PrimaryHover   = Color(0xFFFACC15)
    val PrimaryTint10  = Color(0x1AEAB308)
    // Foreground on light surfaces; Primary remains the brand fill/selection color.
    val AccentTextLight = Color(0xFF8A6300)
    val AccentTextDark  = Color(0xFFFACC15)
    val SwitchCheckedThumbLight = Color(0xFF684900)
    val SwitchCheckedTrackDark = Color(0xFF9A7A28)
    val SwitchCheckedThumbDark = Color(0xFFFFF7D6)

    val GoldWarm       = Color(0xFFD4A84B)
    val GoldWarmHover  = Color(0xFFB68A2E)
    val GoldWarmSoft   = Color(0xFFF8F1DD)

    // ── Semantic ──────────────────────────────────────────────────────────────
    val Success        = Color(0xFF22C55E)
    val Error          = Color(0xFFEF4444)
    val Warning        = Color(0xFFEAB308)
    val Info           = Color(0xFF3B82F6)
    val SuccessTextLight = Color(0xFF166534)
    val ErrorTextLight = Color(0xFFB91C1C)
    val ErrorTextDark = Color(0xFFFCA5A5)
    // Positive affinity has its own semantic role, using brand gold rather than error red.
    val Like           = Primary
    val LikeOutline    = AccentTextLight
    val PhotoBadgeOverlay = Color.Black.copy(alpha = 0.68f)
    val LikePhotoOverlay = PhotoBadgeOverlay

    // ── Light palette ─────────────────────────────────────────────────────────
    val LightBackground    = Color(0xFFFAFAF9)
    val LightSurface       = Color(0xFFFFFFFF)
    val LightSurfaceAlt    = Color(0xFFF9F8F6)
    val LightCard          = Color(0xFFFFFFFF)
    val LightInput         = Color(0xFFFFFFFF)
    val LightBadge         = Color(0xFFF3F4F6)
    val LightTextPrimary   = Color(0xFF1C1917)
    val LightTextSecondary = Color(0xFF78716C)
    val LightTextMuted     = Color(0xFF75706B)
    val LightTextTertiary  = Color(0xFF6B7280)
    val LightTextOnPrimary = Color(0xFF1A1412)
    val LightBorder        = Color(0xFFE7E5E4)
    val LightBorderSubtle  = Color(0xFFE5E1DA)
    val LightBorderHover   = Color(0xFFD1D5DB)
    val LightOverlay       = Color(0x80000000)

    // ── Dark palette ──────────────────────────────────────────────────────────
    val DarkBackground    = Color(0xFF1A1412)
    val DarkSurface       = Color(0xFF2D241F)
    val DarkSurfaceAlt    = Color(0xFF2D241F)
    val DarkCard          = Color(0xFF2D241F)
    val DarkInput         = Color(0xFF1A1412)
    val DarkBadge         = Color(0xFF2D241F)
    val DarkTextPrimary   = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFFA39E93)
    val DarkTextMuted     = Color(0xFFA8A8A8)
    val DarkTextTertiary  = Color(0xFF5C544F)
    val DarkTextOnPrimary = Color(0xFF1A1412)
    val DarkBorder        = Color(0xFF3D2F28)
    val DarkBorderSubtle  = Color(0xFF3D2F28)
    val DarkBorderHover   = Color(0xFF4A3D35)
    val DarkOverlay       = Color(0x80000000)
}
