package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekLikePaletteTest {
    @Test fun selectedUsesGoldRatherThanErrorOrTextColor() {
        assertEquals(CoffeePeekColors.GoldWarmHover, CoffeePeekColors.Like)
        assertEquals(CoffeePeekColors.Like, coffeePeekLikeColor(true, false, Color.White))
        assertEquals(CoffeePeekColors.Like, coffeePeekLikeColor(true, false, Color.Black))
        assertTrue(CoffeePeekColors.Like != CoffeePeekColors.Error)
    }

    @Test fun unselectedUsesThemeContentAndWhiteOnPhotos() {
        assertEquals(Color.Black, coffeePeekLikeColor(false, false, Color.Black))
        assertEquals(Color.White, coffeePeekLikeColor(false, false, Color.White))
        assertEquals(Color.White, coffeePeekLikeColor(false, true, Color.Black))
    }

    @Test fun photoSelectionUsesTheSameGoldAsCardsInEitherTheme() {
        assertEquals(CoffeePeekColors.Like, coffeePeekLikeColor(true, true, Color.Black))
        assertEquals(CoffeePeekColors.Like, coffeePeekLikeColor(true, true, Color.White))
    }

    @Test fun goldAndNeutralRemainDistinctWithReadableSurfaceContrast() {
        for ((surface, neutral) in listOf(CoffeePeekColors.LightSurface to CoffeePeekColors.LightTextPrimary,
            CoffeePeekColors.DarkSurface to CoffeePeekColors.DarkTextPrimary)) {
            val gold = coffeePeekLikeColor(true, false, neutral)
            assertTrue(gold != neutral)
            val high = maxOf(gold.luminance(), surface.luminance())
            val low = minOf(gold.luminance(), surface.luminance())
            assertTrue((high + 0.05f) / (low + 0.05f) >= 3f)
        }
    }

    @Test fun photoPlatePreservesGoldContrastEvenOnWhiteImages() {
        for (photo in listOf(Color.Black, Color.White)) {
            val plate = CoffeePeekColors.LikePhotoOverlay.compositeOver(photo)
            val gold = coffeePeekLikeColor(true, true, Color.White)
            val high = maxOf(gold.luminance(), plate.luminance())
            val low = minOf(gold.luminance(), plate.luminance())
            assertTrue((high + 0.05f) / (low + 0.05f) >= 3f)
        }
    }
}
