package com.coffeepeek.admin.theme

import com.coffeepeek.core.designsystem.theme.CoffeePeekColors

import androidx.compose.ui.graphics.Color

/** Shared by Material and UIKit switches; never derive the thumb from a surface/outline token. */
internal data class SwitchPalette(
    val checkedThumb: Color,
    val checkedTrack: Color,
    val uncheckedThumb: Color,
    val uncheckedTrack: Color,
)

internal fun switchPalette(dark: Boolean) = if (dark) SwitchPalette(
    checkedThumb = CoffeePeekColors.SwitchCheckedThumbDark,
    checkedTrack = CoffeePeekColors.SwitchCheckedTrackDark,
    uncheckedThumb = CoffeePeekColors.SwitchCheckedThumbDark,
    uncheckedTrack = CoffeePeekColors.DarkBorderHover,
) else SwitchPalette(
    checkedThumb = CoffeePeekColors.SwitchCheckedThumbLight,
    checkedTrack = CoffeePeekColors.Primary,
    uncheckedThumb = CoffeePeekColors.LightTextSecondary,
    uncheckedTrack = CoffeePeekColors.LightBadge,
)
