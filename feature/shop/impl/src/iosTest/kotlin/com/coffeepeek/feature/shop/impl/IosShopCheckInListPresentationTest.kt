package com.coffeepeek.feature.shop.impl

import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosShopCheckInListPresentationTest {
    @Test
    fun snapshotChoosesCustomDrinkThenLocalizedCatalogName() {
        val checkIn = checkIn().copy(drinkNameRu = "Фильтр", drinkNameEn = "Filter")
        assertEquals("Filter", row(checkIn, "en-US").drink)
        assertEquals("Фильтр", row(checkIn, "be").drink)
        assertEquals("Filter", row(checkIn.copy(drinkNameRu = " "), "ru").drink)
        assertEquals("Custom", row(checkIn.copy(customDrinkName = "Custom"), "be").drink)
    }

    @Test
    fun snapshotPreparesAvatarRatingsAndPhotoAvailability() {
        val missing = row(checkIn().copy(authorAvatarUrl = " ", photoUrls = listOf(" ")))
        assertFalse(missing.hasAvatar)
        assertNull(missing.ratingLabel)
        assertNull(missing.ratingValue)
        assertEquals(listOf(0, 0, 0), listOf(missing.coffeeRating, missing.serviceRating, missing.placeRating))
        assertFalse(missing.canOpenPhoto(0))

        val present = row(checkIn().copy(authorAvatarUrl = "avatar", rating = ShopRating(place = 3, service = 4, coffee = 5)))
        assertTrue(present.hasAvatar)
        assertEquals("4.0", present.ratingLabel)
        assertEquals(4.0, present.ratingValue)
        assertEquals(listOf(5, 4, 3), listOf(present.coffeeRating, present.serviceRating, present.placeRating))
    }

    @Test
    fun blankOrMissingThumbnailFallsBackToTheFullPhoto() {
        val checkIn = checkIn().copy(photoUrls = listOf("original"), photoThumbnailUrls = listOf(" "))
        assertEquals("original", row(checkIn).thumbnail(0))
        assertEquals("original", row(checkIn.copy(photoThumbnailUrls = emptyList())).thumbnail(0))
        assertEquals("preview", row(checkIn.copy(photoThumbnailUrls = listOf("preview"))).thumbnail(0))
        assertNull(row(checkIn).thumbnail(1))
    }

    private fun row(checkIn: ShopCheckIn, languageTag: String = "ru") =
        ShopCheckInListSnapshot(ShopCheckInListState(checkIns = listOf(checkIn)), languageTag).rows.single()

    private fun checkIn() = ShopCheckIn("check-in", "author", "shop", "", "", "", null,
        emptyList(), emptyList(), null)
}
