package com.coffeepeek.admin.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object CpDimens {

    // ── Spacing ───────────────────────────────────────────────────────────────
    val spacing1  =  4.dp
    val spacing2  =  8.dp
    val spacing3  = 12.dp
    val spacing4  = 16.dp
    val spacing5  = 20.dp
    val spacing6  = 24.dp
    val spacing8  = 32.dp
    val spacing10 = 40.dp
    val spacing12 = 48.dp
    val spacing16 = 64.dp

    // ── Border radius ─────────────────────────────────────────────────────────
    val radiusSm  =  8.dp
    val radiusMd  = 12.dp
    val radiusLg  = 16.dp
    val radiusXl  = 20.dp
    val radius2xl = 24.dp
    val radius3xl = 26.dp
    val radius4xl = 28.dp

    // ── Button / input — one control height, pill ends ────────────────────────
    val controlHeight   = 44.dp
    val buttonHeight    = controlHeight
    val buttonPaddingH  = 16.dp
    val buttonPaddingV  = 10.dp
    val buttonRadius    = controlHeight / 2

    // ── Input ─────────────────────────────────────────────────────────────────
    val inputMinHeight  = controlHeight
    val inputPadding    = 16.dp
    val inputRadius     = buttonRadius
    /** Vertical padding is zero so a 44.dp field does not clip the text line. */
    val singleLineFieldContentPadding = PaddingValues(horizontal = inputPadding, vertical = 0.dp)

    // ── Auth ──────────────────────────────────────────────────────────────────
    val authCardRadius      = 24.dp
    val authCardPadding     = 40.dp
    val authFieldHeight     = controlHeight
    val authFieldRadius     = buttonRadius
    val authPrimaryBtnHeight = controlHeight
    val authMascotSize      = 128.dp
    val authThemeToggleSize = controlHeight
    val authBrandLogoSize   = 96.dp

    // ── Select ────────────────────────────────────────────────────────────────
    val selectMinHeight = 56.dp
    val selectRadius    = 16.dp

    // ── Card ──────────────────────────────────────────────────────────────────
    val cardRadius  = 16.dp
    val cardPadding = 24.dp

    // ── Settings ─────────────────────────────────────────────────────────────
    val settingsPagePadding      = 16.dp
    val settingsSectionSpacing   = 16.dp
    val settingsRowPaddingH      = 16.dp
    val settingsRowPaddingV      = 12.dp
    val settingsIconContainer    = 40.dp
    val settingsIconSize         = 22.dp
    val settingsIconRadius       = 12.dp
    val settingsDividerStart     = 68.dp

    // ── Header ────────────────────────────────────────────────────────────────
    val headerHeight   = 64.dp
    val headerLogoSize = 40.dp
    val headerAvatar   = 30.dp

    // ── Toast ─────────────────────────────────────────────────────────────────
    val toastMinWidth = 320.dp
    val toastRadius   = 12.dp
    val toastPadding  = 16.dp

    // ── Loader ────────────────────────────────────────────────────────────────
    val loaderDefault = 48.dp
    val loaderButton  = 20.dp

    // ── Bottom navigation ─────────────────────────────────────────────────────
    val floatingNavBarHeight = 56.dp
    /** Panel height; the caller adds the system navigation inset. */
    val floatingNavContentClearance = floatingNavBarHeight + 8.dp // + FloatingNavBottomMargin
}
