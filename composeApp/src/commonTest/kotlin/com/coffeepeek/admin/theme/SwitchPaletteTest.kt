package com.coffeepeek.admin.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class SwitchPaletteTest {
    @Test
    fun thumbRemainsDistinctFromTrackInBothThemesAndStates() {
        for (dark in listOf(false, true)) {
            val palette = switchPalette(dark)
            val surface = if (dark) CpColor.DarkSurface else CpColor.LightSurface
            for ((thumb, track) in listOf(palette.checkedThumb to palette.checkedTrack,
                palette.uncheckedThumb to palette.uncheckedTrack)) {
                for (enabled in listOf(false, true)) {
                    val visibleTrack = track.copy(alpha = if (enabled) 1f else .5f).compositeOver(surface)
                    assertTrue(contrast(thumb, visibleTrack) >= 3f,
                        "Invisible switch thumb: dark=$dark, enabled=$enabled")
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
}
