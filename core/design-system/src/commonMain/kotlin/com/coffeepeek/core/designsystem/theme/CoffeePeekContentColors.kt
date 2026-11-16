package com.coffeepeek.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Accent for secondary text and icons on theme surfaces. */
val ColorScheme.coffeePeekSecondaryAccent: Color
    get() = if (surface.luminance() < 0.5f) primary else secondary

/** Foreground over the shop status's translucent green/red fill. */
internal fun ColorScheme.coffeePeekStatusForeground(isOpen: Boolean): Color = when {
    isOpen && surface.luminance() < 0.5f -> CoffeePeekColors.Success
    isOpen -> CoffeePeekColors.SuccessTextLight
    surface.luminance() < 0.5f -> CoffeePeekColors.ErrorTextDark
    else -> CoffeePeekColors.ErrorTextLight
}
