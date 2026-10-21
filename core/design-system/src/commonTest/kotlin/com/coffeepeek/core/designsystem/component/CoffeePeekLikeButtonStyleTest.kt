package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeePeekLikeButtonStyleTest {
    @Test fun compactStylesRetainTheirExistingPlateSize() {
        assertEquals(32.dp, coffeePeekLikePlateSize(CoffeePeekLikeButtonStyle.Plain, 24.dp))
        assertEquals(32.dp, coffeePeekLikePlateSize(CoffeePeekLikeButtonStyle.Glass, 24.dp))
    }

    @Test fun floatingGlassMatchesTheSharedGlassControl() {
        assertEquals(CoffeePeekDimens.buttonHeight,
            coffeePeekLikePlateSize(CoffeePeekLikeButtonStyle.FloatingGlass, 24.dp))
    }

    @Test fun largerGlyphsRetainTheirPaddingInEveryStyle() {
        CoffeePeekLikeButtonStyle.entries.forEach { style ->
            assertEquals(56.dp, coffeePeekLikePlateSize(style, 48.dp))
        }
    }
}
