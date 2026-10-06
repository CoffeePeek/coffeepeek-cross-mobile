package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.domain.model.MapShop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NearbyMapShopsTest {
    @Test
    fun keepsTenNearestInsideRadiusAndRejectsInvalidCoordinates() {
        val origin = GeoPoint(0.0, 0.0)
        val candidates = (12 downTo 1).map { MapShop("$it", "Shop $it", it * 0.001, 0.0) }
        val outside = MapShop("outside", "Far away", 0.05, 0.0)
        val invalid = MapShop("invalid", "Invalid", Double.NaN, 0.0)
        assertEquals((1..10).map(Int::toString), nearestMapShops(candidates + candidates.first() + outside + invalid, origin).map { it.id })
        assertTrue(nearestMapShops(listOf(outside), origin).isEmpty())
        assertTrue(nearestMapShops(emptyList(), origin).isEmpty())
        val bounds = nearbyMapBounds(GeoPoint(53.9, 27.56))
        assertTrue(bounds.minLat < 53.9 && bounds.maxLat > 53.9)
        assertTrue(bounds.minLon < 27.56 && bounds.maxLon > 27.56)
    }

    @Test
    fun carouselStartsWithSelectedShopAndCanScrollBothWays() {
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
