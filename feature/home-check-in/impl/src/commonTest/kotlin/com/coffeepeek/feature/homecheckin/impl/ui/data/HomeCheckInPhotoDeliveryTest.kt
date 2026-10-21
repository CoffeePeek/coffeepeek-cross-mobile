package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import kotlin.test.*

class HomeCheckInPhotoDeliveryTest {
    private fun photo(name: String) = HomePreparationUpload(name, "image/jpeg", byteArrayOf(1))

    @Test fun requestBoundsAndOwnsBytesAndDeliversOnlyOnce() {
        val received = mutableListOf<List<HomePreparationUpload>>()
        val gate = HomeCheckInPhotoDelivery({ true }, received::add)
        val callback = requireNotNull(gate.request(2))
        val source = listOf(photo("first"), photo("second"), photo("third"))
        callback(source); callback(source)
        source.first().bytes[0] = 0
        assertEquals(1, received.size)
        assertEquals(listOf("first", "second"), received.single().map { it.fileName })
        assertContentEquals(byteArrayOf(1), received.single().first().bytes)
        assertNull(gate.request(0))
    }

    @Test fun replacementNeverRetargetsOldCallbackAndEmptyResultDoesNotAddAnything() {
        val received = mutableListOf<List<HomePreparationUpload>>()
        val gate = HomeCheckInPhotoDelivery({ true }, received::add)
        val old = requireNotNull(gate.request(5))
        val fresh = requireNotNull(gate.request(1))
        old(listOf(photo("old"))); fresh(emptyList()); fresh(listOf(photo("late")))
        assertTrue(received.isEmpty())
        requireNotNull(gate.request(99))((1..9).map { photo(it.toString()) })
        assertEquals(5, received.single().size)
    }

    @Test fun busyClosedOrRevokedOwnerCannotLaunchOrDeliverAndDisposalIsPermanent() {
        var canAccept = true
        val received = mutableListOf<List<HomePreparationUpload>>()
        val gate = HomeCheckInPhotoDelivery({ canAccept }, received::add)
        val callback = requireNotNull(gate.request(5))
        canAccept = false; callback(listOf(photo("private")))
        assertNull(gate.request(1))
        canAccept = true; callback(listOf(photo("replayed after busy")))
        assertTrue(received.isEmpty())
        gate.dispose(); canAccept = true; callback(listOf(photo("late")))
        assertNull(gate.request(1))
        assertTrue(received.isEmpty())
    }
}
