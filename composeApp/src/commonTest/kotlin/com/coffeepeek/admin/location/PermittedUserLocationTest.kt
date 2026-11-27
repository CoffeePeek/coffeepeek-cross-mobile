package com.coffeepeek.admin.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PermittedUserLocationTest {
    @Test fun denialNeverReadsOrInventsAPosition() = runTest {
        var reads = 0
        assertNull(readPermittedUserLocation({ false }) { reads++; GeoPoint(1.0, 2.0) })
        assertEquals(0, reads)
    }

    @Test fun returningAfterSettingsGrantCalculatesDistanceFromTheFreshPosition() = runTest {
        var granted = false
        val read = suspend { GeoPoint(0.0, 0.0) }
        assertNull(readPermittedUserLocation({ granted }, read))
        granted = true
        val location = readPermittedUserLocation({ granted }, read)
        assertEquals(111_195.0, distanceToShopMeters(location, 0.0, 1.0) ?: 0.0, 1.0)
    }

    @Test fun permissionRevokedDuringTheReadDiscardsTheOldDistance() = runTest {
        var granted = true
        val location = readPermittedUserLocation({ granted }) {
            granted = false
            GeoPoint(0.0, 0.0)
        }
        assertNull(location)
        assertNull(distanceToShopMeters(location, 0.0, 1.0))
    }

    @Test fun cancellingALocationReadRemainsCancellation() = runTest {
        assertFailsWith<CancellationException> {
            readPermittedUserLocation({ true }) { throw CancellationException("cancelled") }
        }
    }
}
