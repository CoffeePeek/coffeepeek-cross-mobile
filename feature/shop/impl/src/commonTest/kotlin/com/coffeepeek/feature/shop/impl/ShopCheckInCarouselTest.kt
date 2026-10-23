package com.coffeepeek.feature.shop.impl

import com.coffeepeek.feature.shop.impl.ui.compose.component.carouselTargetHeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopCheckInCarouselTest {
    // Cards of 300 px with 12 px between them, 16 px start padding, a 360 px viewport.
    private fun target(heights: List<Int?>, scroll: Int) = carouselTargetHeight(heights, scroll, viewport = 360, card = 300, spacing = 12, padding = 16)

    @Test fun nothingIsDecidedUntilEveryCardIsMeasured() {
        assertNull(target(listOf(200, null, 500), 0))
        assertNull(target(emptyList(), 0))
    }

    @Test fun theHeightFollowsTheTallestCardThatIsAtLeastPartlyOnScreen() {
        val heights = listOf(200, 500, 300)
        // At rest the first card and a peek of the second are visible.
        assertEquals(500, target(heights, 0))
        // The second card at the start edge and a peek of the third.
        assertEquals(500, target(heights, 328))
        // The tall card is still partly visible while it leaves.
        assertEquals(500, target(heights, 400))
        // Only the last card is left on screen.
        assertEquals(300, target(heights, 700))
    }

    @Test fun aShortCardAloneKeepsItsOwnHeight() {
        // Scrolled so far that only the first (short) card is on screen.
        assertEquals(200, carouselTargetHeight(listOf(200), scroll = 0, viewport = 360, card = 300, spacing = 12, padding = 16))
        assertEquals(300, target(listOf(200, 500, 300), 900))
    }
}
