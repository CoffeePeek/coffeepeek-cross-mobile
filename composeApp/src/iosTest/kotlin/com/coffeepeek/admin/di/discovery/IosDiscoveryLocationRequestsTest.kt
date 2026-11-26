package com.coffeepeek.admin.di.discovery

import kotlin.test.*

class IosDiscoveryLocationRequestsTest {
    @Test fun grantingPermissionLaterDoesNotRecenterTheAlreadyViewedMap() {
        val requests = IosDiscoveryLocationRequests()
        assertFalse(requests.take(0, permitted = false))
        assertFalse(requests.take(0, permitted = true))
    }

    @Test fun anAlreadyAuthorizedNewMapCanUseTheInitialPositionOnlyOnce() {
        val requests = IosDiscoveryLocationRequests()
        assertTrue(requests.take(0, permitted = true))
        assertFalse(requests.take(0, permitted = true))
    }

    @Test fun restoredViewportDoesNotReplayAnOlderAccountViewsLocationRequest() {
        val requests = IosDiscoveryLocationRequests()
        requests.restore(3)
        assertFalse(requests.take(3, permitted = true))
        assertTrue(requests.take(4, permitted = true))
    }

    @Test fun explicitLocationRequestsWaitForPermissionAndEachKeyRunsOnlyOnce() {
        val requests = IosDiscoveryLocationRequests()
        assertFalse(requests.take(0, permitted = false))
        assertFalse(requests.take(1, permitted = false))
        assertTrue(requests.take(1, permitted = true))
        assertFalse(requests.take(1, permitted = true))
        assertTrue(requests.take(2, permitted = true))
    }
}
