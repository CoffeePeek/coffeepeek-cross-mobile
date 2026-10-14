package com.coffeepeek.admin.theme

import androidx.compose.ui.graphics.Color

/** Shared by Material and UIKit switches; never derive the thumb from a surface/outline token. */
internal data class SwitchPalette(
    val checkedThumb: Color,
    val checkedTrack: Color,
    val uncheckedThumb: Color,
    val uncheckedTrack: Color,
)

internal fun switchPalette(dark: Boolean) = if (dark) SwitchPalette(
    checkedThumb = CpColor.SwitchCheckedThumbDark,
    checkedTrack = CpColor.SwitchCheckedTrackDark,
    uncheckedThumb = CpColor.SwitchCheckedThumbDark,
    uncheckedTrack = CpColor.DarkBorderHover,
) else SwitchPalette(
    checkedThumb = CpColor.SwitchCheckedThumbLight,
    checkedTrack = CpColor.Primary,
    uncheckedThumb = CpColor.LightTextSecondary,
    uncheckedTrack = CpColor.LightBadge,
)
