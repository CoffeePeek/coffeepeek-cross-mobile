package com.coffeepeek.admin.map

import com.coffeepeek.domain.model.MapBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapMarkerTransitionTest {
    @Test fun splitAndMergeUseTheSmallestContainingGroupRegardlessOfOrderOrServerId() {
        val shop = MapMarkerTarget("shop:a", 1.0, 1.0)
        val city = MapMarkerTarget("cluster:city", 1.0, 1.0, MapBounds(-2.0, -2.0, 4.0, 4.0))
        val block = MapMarkerTarget("cluster:block", 1.0, 1.0, MapBounds(0.0, 0.0, 2.0, 2.0))
        assertEquals(block, markerParent(shop, listOf(city, block)))
        assertEquals(block, markerParent(shop, listOf(block, city)))
        assertEquals(block, markerParent(city, listOf(block)))
        assertEquals(city, markerParent(block, listOf(city)))
    }

    @Test fun unrelatedPointsAndTheSameMarkerDoNotBecomeParents() {
        val shop = MapMarkerTarget("shop:a", 5.0, 5.0)
        val group = MapMarkerTarget("cluster:b", 1.0, 1.0, MapBounds(0.0, 0.0, 2.0, 2.0))
        assertNull(markerParent(shop, listOf(group)))
        assertNull(markerParent(group, listOf(group, shop)))
        assertEquals(group, markerParent(shop.copy(latitude = 2.0, longitude = 2.0), listOf(group)))
    }
}
