package com.coffeepeek.admin.ui.favorites

import com.coffeepeek.admin.location.GeoPoint
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class FavoritesDistanceLabelTest {
    @Test fun formatsSavedCoordinatesUsingExistingAppDistanceRules() {
        assertEquals("111,2 км", favoriteDistanceLabel(GeoPoint(0.0, 0.0), 0.0, 1.0))
    }

    @Test fun hidesDistanceWithoutPermissionOrForInvalidCoordinates() {
        assertNull(favoriteDistanceLabel(null, 0.0, 1.0))
        assertNull(favoriteDistanceLabel(GeoPoint(0.0, 0.0), 91.0, 1.0))
    }
}
