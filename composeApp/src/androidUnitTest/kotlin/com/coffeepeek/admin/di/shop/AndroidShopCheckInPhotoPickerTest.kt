package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AndroidShopCheckInPhotoPickerTest {
    @Test fun mapsPreparedPhotosAndCapsDeliveryAtRemainingSlots() {
        val images = (1..8).map { PickedImage(byteArrayOf(it.toByte()), "$it.jpg", "image/jpeg") }
        assertEquals(listOf(ShopCheckInPhoto(byteArrayOf(1), "1.jpg"), ShopCheckInPhoto(byteArrayOf(2), "2.jpg")),
            images.toCheckInPhotos(2))
        assertEquals(5, images.toCheckInPhotos(99).size)
        assertEquals(emptyList(), images.toCheckInPhotos(-1))
        assertEquals(emptyList(), images.toCheckInPhotos(0))
        assertSame(images.first().bytes, images.toCheckInPhotos(1).single().bytes)
    }
}
