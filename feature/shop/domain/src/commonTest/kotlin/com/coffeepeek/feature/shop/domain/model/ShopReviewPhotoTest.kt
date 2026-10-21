package com.coffeepeek.feature.shop.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopReviewPhotoTest {
    @Test fun selectionNeverExceedsFiveIncludingWhenPickerReturnsTooMany() {
        val current = (1..4).map { ShopReviewPhoto(byteArrayOf(it.toByte()), "$it.jpg") }
        val picked = (5..7).map { ShopReviewPhoto(byteArrayOf(it.toByte()), "$it.jpg") }
        assertEquals(listOf("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg"),
            appendShopReviewPhotos(current, picked).map(ShopReviewPhoto::fileName))
        assertEquals(MAX_SHOP_REVIEW_PHOTOS, appendShopReviewPhotos(emptyList(), picked + current).size)
    }
}
