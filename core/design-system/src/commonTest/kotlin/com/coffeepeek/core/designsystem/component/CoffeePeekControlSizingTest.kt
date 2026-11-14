package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoffeePeekControlSizingTest {
    @Test fun segmentsScrollBeforeTargetsBecomeTooSmall() {
        assertFalse(coffeePeekSegmentsNeedScroll(2, 320.dp, 1f, false))
        assertTrue(coffeePeekSegmentsNeedScroll(8, 320.dp, 1f, false))
        assertTrue(coffeePeekSegmentsNeedScroll(2, 320.dp, 2f, false))
        assertTrue(coffeePeekSegmentsNeedScroll(2, 320.dp, 1f, true))
    }

    @Test fun segmentBoundaryIncludesBothTrackInsets() {
        val available = CoffeePeekDimens.minimumTouchTarget * 3 + 8.dp
        assertFalse(coffeePeekSegmentsNeedScroll(3, available, 1f, false))
        assertTrue(coffeePeekSegmentsNeedScroll(3, available - 1.dp, 1f, false))
    }

    @Test fun ratingBudgetPreservesFiveActionTargetsAndSpacing() {
        val width = coffeePeekRatingInputWidth(2.dp)
        assertTrue(width >= 228.dp)
        assertEquals(CoffeePeekDimens.minimumTouchTarget, (width - 8.dp) / 5)
    }

    @Test fun priceArtworkHasStableBoxesForCompactAndRegularUses() {
        assertEquals(10.dp, coffeePeekBynIndicatorSize(8.dp).height)
        assertEquals(14.dp, coffeePeekBynIndicatorSize(10.dp).height)
        assertEquals(16.dp, coffeePeekBynIndicatorSize(12.dp).height)
        assertEquals(18.dp, coffeePeekBynIndicatorSize(14.dp).height)
        assertEquals(14.dp, coffeePeekBynIndicatorSize(14.dp).width)
    }
}
