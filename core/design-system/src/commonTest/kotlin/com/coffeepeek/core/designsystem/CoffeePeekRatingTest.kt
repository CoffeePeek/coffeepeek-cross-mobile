package com.coffeepeek.core.designsystem

import com.coffeepeek.core.designsystem.component.coffeePeekRatingStarFill
import com.coffeepeek.core.designsystem.component.coffeePeekRatingText
import com.coffeepeek.core.designsystem.component.coffeePeekRatingValue
import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeePeekRatingTest {
    @Test fun fractionalRatingsNeverTruncateTheLastStar() {
        for (value in listOf(0.0, 0.1, 1.0, 2.7, 3.5, 4.3, 4.9, 5.0)) {
            val fills = (0..4).map { coffeePeekRatingStarFill(value, it) }
            assertEquals(value, fills.sumOf { it.toDouble() }, 0.000001)
            for ((index, fill) in fills.withIndex()) {
                assertEquals((value - index).coerceIn(0.0, 1.0).toFloat(), fill, 0.000001f)
            }
        }
    }

    @Test fun textAndStarsUseTheSameRoundedTenth() {
        assertEquals(4.3, coffeePeekRatingValue(13.0 / 3))
        assertEquals("4.3", coffeePeekRatingText(13.0 / 3))
        assertEquals(0.3f, coffeePeekRatingStarFill(13.0 / 3, 4), 0.000001f)
        assertEquals("4.7", coffeePeekRatingText(14.0 / 3))
        assertEquals("5.0", coffeePeekRatingText(4.99))
    }

    @Test fun outOfRangeAndNonFiniteValuesAreSafe() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0)) {
            assertEquals(0.0, coffeePeekRatingValue(value))
            assertEquals("0.0", coffeePeekRatingText(value))
        }
        assertEquals(5.0, coffeePeekRatingValue(Double.MAX_VALUE))
        assertEquals("5.0", coffeePeekRatingText(6.0))
    }
}
