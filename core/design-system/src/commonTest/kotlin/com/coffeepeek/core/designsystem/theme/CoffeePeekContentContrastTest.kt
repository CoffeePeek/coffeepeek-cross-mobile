package com.coffeepeek.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class CoffeePeekContentContrastTest {
    @Test fun primaryIsReadableOnDarkThemeSurfaces() {
        with(CoffeePeekDarkColorScheme) {
            listOf(background, surface, surfaceVariant).forEach { assertContrast(primary, it, 4.5f) }
        }
    }

    @Test fun statusTextIsReadableOnItsActualCompositedFill() {
        for (surface in listOf(CoffeePeekColors.LightSurface, CoffeePeekColors.DarkSurface)) {
            for (open in listOf(true, false)) {
                val foreground = lightColorScheme(surface = surface).coffeePeekStatusForeground(open)
                val fill = if (open) CoffeePeekColors.Success else foreground
                assertContrast(foreground, fill.copy(alpha = 0.14f).compositeOver(surface), 4.5f)
            }
        }
    }

    @Test fun filledBrandControlsKeepReadableDarkLabels() {
        assertContrast(CoffeePeekColors.LightTextOnPrimary, CoffeePeekColors.Primary, 4.5f)
        assertContrast(CoffeePeekColors.DarkTextOnPrimary, CoffeePeekColors.Primary, 4.5f)
    }

    private fun assertContrast(foreground: Color, background: Color, minimum: Float) {
        val high = maxOf(foreground.luminance(), background.luminance())
        val low = minOf(foreground.luminance(), background.luminance())
        val ratio = (high + 0.05f) / (low + 0.05f)
        assertTrue(ratio >= minimum, "Contrast $ratio is below $minimum: $foreground on $background")
    }

}
