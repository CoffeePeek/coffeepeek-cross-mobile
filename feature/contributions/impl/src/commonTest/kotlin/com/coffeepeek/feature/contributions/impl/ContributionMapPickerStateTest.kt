@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.impl.ui.map.ContributionMapPickerState
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class ContributionMapPickerStateTest {
    private val a = GeoPoint(53.9, 27.5)
    private val b = GeoPoint(54.0, 28.0)

    @Test fun cameraTargetIsConsumedOnceAndLocationRequestsAreExplicit() {
        val picker = ContributionMapPickerState(a)
        assertEquals(a, picker.state.cameraTarget)
        assertEquals(0, picker.state.locationRequest)
        picker.cameraApplied()
        picker.requestLocation()
        picker.requestLocation()
        assertNull(picker.state.cameraTarget)
        assertEquals(2, picker.state.locationRequest)
    }

    @Test fun subMeterNativeDriftAndInvalidCoordinatesDoNotChangeSelection() {
        val picker = ContributionMapPickerState(a)
        picker.move(GeoPoint(a.latitude + 0.0000001, a.longitude - 0.0000001))
        assertEquals(a, picker.state.point)
        for (point in listOf(GeoPoint(Double.NaN, 0.0), GeoPoint(91.0, 0.0), GeoPoint(0.0, 181.0))) picker.move(point)
        assertEquals(a, picker.state.point)
        picker.move(b)
        assertEquals(b, picker.state.point)
    }

    @Test fun resolvingDebouncesAndConfirmationRequiresTheCurrentTrimmedAddress() = runTest {
        val picker = ContributionMapPickerState(a)
        var calls = 0
        val task = launch { picker.resolve(location { calls++; "  Street 1  " }) }
        runCurrent()
        assertTrue(picker.state.isResolving)
        assertNull(picker.selection())
        advanceTimeBy(349)
        assertEquals(0, calls)
        advanceTimeBy(1)
        runCurrent()
        task.join()
        assertEquals(1, calls)
        assertEquals(a to "Street 1", picker.selection())
        picker.move(b)
        assertNull(picker.selection())
    }

    @Test fun lateGeocodeForAnotherPointCannotReplaceTheNewAddress() = runTest {
        val gate = CompletableDeferred<String>()
        val picker = ContributionMapPickerState(a)
        val first = launch { picker.resolve(location { gate.await() }) }
        advanceTimeBy(350)
        runCurrent()
        picker.move(b)
        picker.resolve(location { "New address" })
        gate.complete("Old address")
        first.join()
        assertEquals(b to "New address", picker.selection())
        assertFalse(picker.state.isResolving)
    }

    @Test fun cancelledNonCooperativeGeocodeNeverPublishesItsResult() = runTest {
        val gate = CompletableDeferred<String>()
        val picker = ContributionMapPickerState(a)
        val task = launch { picker.resolve(location { withContext(NonCancellable) { gate.await() } }) }
        advanceTimeBy(350)
        runCurrent()
        task.cancel()
        gate.complete("Late address")
        task.join()
        assertTrue(task.isCancelled)
        assertNull(picker.selection())
        assertNull(picker.state.address)
        assertFalse(picker.state.isResolving)
    }

    @Test fun missingOrFailedAddressKeepsConfirmationDisabledAndCancellationEscapes() = runTest {
        val picker = ContributionMapPickerState(a)
        for (read in listOf<suspend (GeoPoint) -> String?>({ null }, { throw IllegalStateException("offline") })) {
            picker.resolve(location(read))
            assertTrue(picker.state.failed)
            assertNull(picker.selection())
        }
        assertFailsWith<CancellationException> { picker.resolve(location { throw CancellationException("cancel") }) }
    }

    @Test fun anEmptyInitialPointDoesNotGeocodeOrInventAnAddress() = runTest {
        val picker = ContributionMapPickerState(null)
        picker.resolve(location { error("must wait for a real camera center") })
        assertNull(picker.selection())
        assertFalse(picker.state.isResolving)
        assertFalse(picker.state.failed)
    }

    private fun location(read: suspend (GeoPoint) -> String?) = object : ContributionLocation {
        override suspend fun lastKnown(): GeoPoint? = error("No device location in geocoding")
        override suspend fun addressOf(point: GeoPoint) = read(point)
    }
}
