package com.coffeepeek.feature.shop.domain.usecase

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import kotlin.time.Instant
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidateShopCheckInTest {
    private val now = Instant.parse("2026-10-08T12:00:00Z")
    private val privateInput = ShopCheckInCreateInput("shop-1", "A visit", ShopRating(4, 4, 4),
        "2026-10-06T12:00:00Z")

    @Test fun privateAndPublicCheckInsUseTheSameTextAndRatingRules() {
        for (visibility in ShopCheckInVisibility.entries) {
            val input = privateInput.copy(visibility = visibility)
            assertNull(validateShopCheckIn(input, now))
            assertNull(validateShopCheckIn(input.copy(text = ""), now))
            assertNull(validateShopCheckIn(input.copy(text = " "), now))
            assertNull(validateShopCheckIn(input.copy(text = "x".repeat(1000)), now))
            assertEquals(ShopCheckInValidationError.InvalidNote,
                validateShopCheckIn(input.copy(text = "x".repeat(1001)), now))
            assertEquals(ShopCheckInValidationError.InvalidRating,
                validateShopCheckIn(input.copy(rating = ShopRating(0, 4, 4)), now))
            assertEquals(ShopCheckInValidationError.InvalidRating,
                validateShopCheckIn(input.copy(rating = ShopRating(4, 6, 4)), now))
        }
    }

    @Test fun malformedMissingAndFutureVisitDatesAreRejected() {
        for (value in listOf("", "not-a-date", "2026-02-30T12:00:00Z", "1970-01-01T00:00:00Z",
            "2026-10-08T12:00:01Z")) {
            assertEquals(ShopCheckInValidationError.InvalidVisitDate,
                validateShopCheckIn(privateInput.copy(visitedAtIso = value), now), value)
        }
        assertNull(validateShopCheckIn(privateInput.copy(visitedAtIso = now.toString()), now))
    }

    @Test fun customDrinkAndPhotoLimitsMatchCurrentFlow() {
        assertEquals(ShopCheckInValidationError.InvalidDrink,
            validateShopCheckIn(privateInput.copy(drinkSlug = "other"), now))
        assertEquals(ShopCheckInValidationError.InvalidDrink,
            validateShopCheckIn(privateInput.copy(drinkSlug = "espresso", customDrinkName = "Custom"), now))
        assertNull(validateShopCheckIn(privateInput.copy(drinkSlug = "other", customDrinkName = "Flat white"), now))
        val photo = ShopCheckInPhoto(byteArrayOf(1), "visit.jpg")
        assertEquals(ShopCheckInValidationError.TooManyPhotos,
            validateShopCheckIn(privateInput.copy(photos = List(6) { photo }), now))
        assertNull(validateShopCheckIn(privateInput.copy(photos = List(5) { photo }), now))
        assertEquals(ShopCheckInValidationError.InvalidPhoto,
            validateShopCheckIn(privateInput.copy(photos = listOf(photo.copy(bytes = byteArrayOf()))), now))
    }

    @Test fun invalidShopSlugIsRejected() {
        for (slug in listOf("", " ", "bad/id", "bad?query", "bad#fragment")) {
            assertEquals(ShopCheckInValidationError.InvalidShop,
                validateShopCheckIn(privateInput.copy(shopSlug = slug), now))
        }
    }

    @Test fun cutoutRequiresAnOriginalAndDoesNotConsumeTheFullPhotoLimit() {
        val png = drinkPhoto()
        assertEquals(ShopCheckInValidationError.InvalidDrinkPhoto,
            validateShopCheckIn(privateInput.copy(drinkPhoto = png), now))
        assertNull(validateShopCheckIn(privateInput.copy(
            photos = List(5) { ShopCheckInPhoto(byteArrayOf(1), "visit.jpg") }, drinkPhoto = png), now))
    }

    @Test fun cutoutHeaderTypeAndSizeAreValidatedBeforeUpload() {
        val png = drinkPhoto()
        val invalid = listOf(
            png.copy(contentType = "image/jpeg"), png.copy(fileName = " "),
            png.copy(bytes = png.bytes.copyOf(32)),
            png.copy(bytes = png.bytes.copyOf().apply { this[0] = 0 }),
            png.copy(bytes = png.bytes.copyOf().apply { this[19] = 0 }),
            png.copy(bytes = png.bytes.copyOf().apply { this[18] = 3; this[19] = 1 }),
            png.copy(bytes = png.bytes.copyOf().apply { this[20] = -1 }),
            png.copy(bytes = png.bytes.copyOf(4 * 1024 * 1024 + 1)),
        )
        for (photo in invalid) assertEquals(ShopCheckInValidationError.InvalidDrinkPhoto,
            validateShopCheckIn(privateInput.copy(photos = listOf(ShopCheckInPhoto(byteArrayOf(1), "visit.jpg")),
                drinkPhoto = photo), now))
    }

    private fun drinkPhoto() = ShopCheckInPhoto(Base64.decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/d1sAAAAASUVORK5CYII="),
        "drink.png", "image/png")
}
