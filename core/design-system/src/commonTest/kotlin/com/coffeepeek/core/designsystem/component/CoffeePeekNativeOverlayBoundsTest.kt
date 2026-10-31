package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoffeePeekNativeOverlayBoundsTest {
    @Test fun fullyVisibleControlDoesNotMaskItsGlassOrShadow() {
        assertNull(coffeePeekNativeOverlayBounds(Rect(24f, 100f, 72f, 148f),
            Offset(24f, 100f), Size(48f, 48f)))
    }

    @Test fun scrollingBeneathHeaderClipsTheTopInControlCoordinates() {
        assertEquals(Rect(0f, 20f, 48f, 48f), coffeePeekNativeOverlayBounds(
            Rect(24f, 120f, 72f, 148f), Offset(24f, 100f), Size(48f, 48f)))
    }

    @Test fun horizontalCarouselClipsTrailingEdge() {
        assertEquals(Rect(0f, 0f, 18f, 48f), coffeePeekNativeOverlayBounds(
            Rect(342f, 100f, 360f, 148f), Offset(342f, 100f), Size(48f, 48f)))
    }

    @Test fun controlOutsideViewportIsHiddenInsteadOfFloatingAboveHeader() {
        assertTrue(checkNotNull(coffeePeekNativeOverlayBounds(Rect.Zero,
            Offset(24f, -60f), Size(48f, 48f))).isEmpty)
    }
}
