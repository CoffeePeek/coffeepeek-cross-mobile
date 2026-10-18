package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekLikePaletteTest {
    @Test fun selectedUsesGoldRatherThanErrorOrTextColor() {
        assertEquals(CoffeePeekColors.Like, coffeePeekLikeColor(true, false, Color.Black, Color.White))
        assertEquals(CoffeePeekColors.LikeOnLight, coffeePeekLikeColor(true, false, Color.White, Color.Black))
        assertTrue(CoffeePeekColors.Like != CoffeePeekColors.Error)
    }

    @Test fun unselectedUsesThemeContentAndWhiteOnPhotos() {
        assertEquals(Color.Black, coffeePeekLikeColor(false, false, Color.White, Color.Black))
        assertEquals(Color.White, coffeePeekLikeColor(false, false, Color.Black, Color.White))
        assertEquals(Color.White, coffeePeekLikeColor(false, true, Color.White, Color.Black))
    }

    @Test fun photoSelectionUsesBrightBrandGoldInEitherTheme() {
        assertEquals(CoffeePeekColors.Primary, coffeePeekLikeColor(true, true, Color.White, Color.Black))
        assertEquals(CoffeePeekColors.Primary, coffeePeekLikeColor(true, true, Color.Black, Color.White))
    }

    @Test fun goldAndNeutralRemainDistinctWithReadableSurfaceContrast() {
        for ((surface, neutral) in listOf(CoffeePeekColors.LightSurface to CoffeePeekColors.LightTextPrimary,
            CoffeePeekColors.DarkSurface to CoffeePeekColors.DarkTextPrimary)) {
            val gold = coffeePeekLikeColor(true, false, surface, neutral)
            assertTrue(gold != neutral)
            val high = maxOf(gold.luminance(), surface.luminance())
            val low = minOf(gold.luminance(), surface.luminance())
            assertTrue((high + 0.05f) / (low + 0.05f) >= 3f)
        }
    }
}
