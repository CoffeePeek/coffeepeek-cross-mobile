package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.impl.ui.carouselStartPage
import kotlin.test.Test
import kotlin.test.assertEquals

class MapCarouselStartTest {
    @Test fun carouselStartsWithTheSelectedShopAndCanScrollBothWays() {
        for (count in 2..10) {
            for (selected in 0 until count) {
                val start = carouselStartPage(count, selected)
                assertEquals(selected, start % count)
                assertEquals((selected + count - 1) % count, (start - 1) % count)
                assertEquals((selected + 1) % count, (start + 1) % count)
            }
        }
        assertEquals(0, carouselStartPage(1, 0))
        // No selected shop: show the first card and allow swiping in either direction.
        val unfocusedStart = carouselStartPage(10, -1)
        assertEquals(0, unfocusedStart % 10)
        assertEquals(9, (unfocusedStart - 1) % 10)
        assertEquals(1, (unfocusedStart + 1) % 10)
    }
}
