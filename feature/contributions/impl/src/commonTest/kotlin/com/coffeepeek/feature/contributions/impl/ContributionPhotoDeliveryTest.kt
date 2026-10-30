package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.api.ContributionPhotoPicker
import com.coffeepeek.feature.contributions.api.ContributionPhotoPickerController
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.impl.ui.component.ContributionPhotoDelivery
import kotlin.test.*

class ContributionPhotoDeliveryTest {
    private val photo = PendingPhoto("photo.jpg", "image/jpeg", byteArrayOf(1, 2))
    private val picker = ContributionPhotoPicker { ContributionPhotoPickerController(false, { _, _ -> }, {}) }

    @Test fun galleryDeliveryUsesBothLaunchLimitAndRemainingCapacityAndOwnsItsBytes() {
        var remaining = 2
        val accepted = mutableListOf<List<PendingPhoto>>()
        val delivery = ContributionPhotoDelivery({ remaining }, accepted::add)
        val callback = delivery.request(2)!!
        remaining = 1
        callback(listOf(photo, photo, photo))
        assertEquals(1, accepted.single().size)
        photo.bytes[0] = 9
        assertContentEquals(byteArrayOf(1, 2), accepted.single().single().bytes)
        callback(listOf(photo))
        assertEquals(1, accepted.size)
    }

    @Test fun newerRequestAndDisposalRejectLateCallbacksAndZeroCapacity() {
        val accepted = mutableListOf<List<PendingPhoto>>()
        val delivery = ContributionPhotoDelivery({ 5 }, accepted::add)
        assertNull(delivery.request(0))
        val old = delivery.request(1)!!
        val current = delivery.request(1)!!
        old(listOf(photo))
        assertTrue(accepted.isEmpty())
        delivery.dispose()
        current(listOf(photo))
        assertTrue(accepted.isEmpty())
        assertNull(delivery.request(1))
    }

    @Test fun guestOrReplacedAccountCannotReceivePendingPhotos() {
        val account = FakeAccount(null)
        val bound = AccountContributionPhotoPicker(picker, account)
        val accepted = mutableListOf<List<PendingPhoto>>()
        assertNull(bound.delivery(accepted::add))
        account.key.value = "one"
        val deliver = bound.delivery(accepted::add)!!
        account.key.value = "two"
        deliver(listOf(photo))
        assertTrue(accepted.isEmpty())
        bound.delivery(accepted::add)!!(listOf(photo))
        assertEquals(listOf(listOf(photo)), accepted)
    }
}
