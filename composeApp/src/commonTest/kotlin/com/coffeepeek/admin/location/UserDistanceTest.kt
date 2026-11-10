package com.coffeepeek.admin.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserDistanceTest {

    @Test
    fun calculatesGreatCircleDistance() {
        val distance = distanceToShopMeters(
            userLocation = GeoPoint(latitude = 0.0, longitude = 0.0),
            latitude = 0.0, longitude = 1.0,
        )

        assertEquals(expected = 111_195.0, actual = distance ?: 0.0, absoluteTolerance = 1.0)
    }

    @Test
    fun skipsDistanceWhenCoordinatesAreMissingOrInvalid() {
        assertNull(distanceToShopMeters(null, 1.0, 1.0))
        assertNull(distanceToShopMeters(GeoPoint(1.0, 1.0), null, 1.0))
        assertNull(distanceToShopMeters(GeoPoint(91.0, 1.0), 1.0, 1.0))
    }

    @Test
    fun formatsMetersAndKilometersForRussianUi() {
        assertEquals("850 м", formatDistance(850.4))
        assertEquals("1,2 км", formatDistance(1_249.0))
        assertEquals("2 км", formatDistance(1_999.0))
        assertNull(formatDistance(null))
    }
}
