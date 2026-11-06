@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Fake platform callbacks only: no CoreLocation, permissions dialog or device. */
class AppDiscoveryLocationTest {
    @BeforeTest fun mainDispatcher() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun resetDispatcher() { Dispatchers.resetMain() }

    @Test fun deniedPermissionNeverReadsLocation() = runTest {
        var reads = 0
        val location = AppDiscoveryLocation(backgroundScope, { false }) { reads++; GeoPoint(1.0, 2.0) }
        runCurrent()
        assertEquals(0, reads)
        assertNull(location.current.value)
    }

    @Test fun grantedPermissionReadsOnTheMainDispatcherAndRefreshes() = runTest {
        var point = GeoPoint(1.0, 2.0)
        val location = AppDiscoveryLocation(backgroundScope, { true }) { point }
        runCurrent()
        assertEquals(Coordinates(1.0, 2.0), location.current.value)
        point = GeoPoint(3.0, 4.0)
        location.refresh()
        runCurrent()
        assertEquals(Coordinates(3.0, 4.0), location.current.value)
    }

    @Test fun permissionRevokedDuringTheReadDropsThePosition() = runTest {
        var granted = true
        val location = AppDiscoveryLocation(backgroundScope, { granted }) {
            granted = false
            GeoPoint(1.0, 2.0)
        }
        runCurrent()
        assertNull(location.current.value)
    }

    @Test fun anUnavailablePositionClearsThePreviousOne() = runTest {
        var point: GeoPoint? = GeoPoint(1.0, 2.0)
        val location = AppDiscoveryLocation(backgroundScope, { true }) { point }
        runCurrent()
        assertEquals(Coordinates(1.0, 2.0), location.current.value)
        point = null
        location.refresh()
        runCurrent()
        assertNull(location.current.value)
    }
}
