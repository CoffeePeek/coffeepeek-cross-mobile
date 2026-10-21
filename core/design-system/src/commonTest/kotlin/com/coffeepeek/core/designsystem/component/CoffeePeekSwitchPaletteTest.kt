package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekSwitchPaletteTest {
    @Test fun thumbRemainsDistinctInBothThemesStatesAndDisabledModes() {
        for (dark in listOf(false, true)) {
            val palette = coffeePeekSwitchPalette(dark)
            val surface = if (dark) CoffeePeekColors.DarkSurface else CoffeePeekColors.LightSurface
            for ((thumb, track) in listOf(palette.checkedThumb to palette.checkedTrack,
                palette.uncheckedThumb to palette.uncheckedTrack)) {
                for (enabled in listOf(false, true)) {
                    val visibleTrack = track.copy(alpha = if (enabled) 1f else 0.5f).compositeOver(surface)
                    val contrast = (maxOf(thumb.luminance(), visibleTrack.luminance()) + 0.05f) /
                        (minOf(thumb.luminance(), visibleTrack.luminance()) + 0.05f)
                    assertTrue(contrast >= 3f, "Thumb lost contrast: dark=$dark enabled=$enabled")
                }
            }
        }
    }

    @Test fun explicitThumbColorsMatchTheProductionTokens() {
        assertEquals(Color(0xFFFFF7D6), coffeePeekSwitchPalette(true).uncheckedThumb)
        assertEquals(Color(0xFF78716C), coffeePeekSwitchPalette(false).uncheckedThumb)
        assertEquals(Color(0xFF684900), coffeePeekSwitchPalette(false).checkedThumb)
    }
}
