package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.PickedImage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class NativeShopPhotoOwnershipTest {
    @Test fun theDraftRetainsItsOwnBytesWhenTheNativePickerWipesItsTemporaryBuffer() {
        val bytes = byteArrayOf(1, 2, 3)
        val photos = listOf(PickedImage(bytes, "photo.jpg", "image/jpeg")).toCheckInPhotos(1)
        bytes.fill(0)
        assertEquals("photo.jpg", photos.single().fileName)
        assertContentEquals(byteArrayOf(1, 2, 3), photos.single().bytes)
    }
}
