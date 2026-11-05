package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.location.GeoPoint as PlatformPoint
import com.coffeepeek.feature.contributions.api.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppContributionLocationTest {
    @Test fun deniedPermissionDoesNotReadDevicePosition() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val location = AppContributionLocation({ false }, { error("Position must not be read") })
            assertNull(location.lastKnown())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun permittedPositionIsMapped() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val location = AppContributionLocation({ true }, { PlatformPoint(53.9, 27.5) })
            assertEquals(GeoPoint(53.9, 27.5), location.lastKnown())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun permissionRevokedDuringReadDropsPosition() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var permitted = true
            val location = AppContributionLocation({ permitted }, {
                permitted = false
                PlatformPoint(53.9, 27.5)
            })
            assertNull(location.lastKnown())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun manualMapPointDoesNotRequireLocationPermission() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val location = AppContributionLocation({ false }, { error("No device read") }, { lat, lon -> "$lat,$lon" })
            assertEquals("53.9,27.5", location.addressOf(GeoPoint(53.9, 27.5)))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun cancellationIsNotConvertedToAnEmptyPositionOrAddress() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val location = AppContributionLocation({ true }, { throw CancellationException() }, { _, _ -> throw CancellationException() })
            assertFailsWith<CancellationException> { location.lastKnown() }
            assertFailsWith<CancellationException> { location.addressOf(GeoPoint(0.0, 0.0)) }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
