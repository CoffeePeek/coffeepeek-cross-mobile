package com.coffeepeek.feature.shop.domain.usecase

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidateShopCheckInTest {
    private val privateInput = ShopCheckInCreateInput("shop-1", "2026-10-06T12:00:00Z", false)

    @Test fun privateCheckInNeedsNoPublicTextOrRating() {
        assertNull(validateShopCheckIn(privateInput))
    }

    @Test fun publicCheckInRequiresValidTitleAndDescription() {
        assertEquals(ShopCheckInValidationError.InvalidHeader,
            validateShopCheckIn(privateInput.copy(isPublic = true)))
        assertEquals(ShopCheckInValidationError.InvalidNote,
            validateShopCheckIn(privateInput.copy(isPublic = true, header = "Coffee")))
        assertNull(validateShopCheckIn(privateInput.copy(isPublic = true, header = "Coffee",
            note = "A good visit", rating = ShopRating(5, 4, 5))))
    }

    @Test fun customDrinkAndPhotoLimitsMatchCurrentFlow() {
        assertEquals(ShopCheckInValidationError.InvalidDrink,
            validateShopCheckIn(privateInput.copy(drinkSlug = "other")))
        assertEquals(ShopCheckInValidationError.InvalidDrink,
            validateShopCheckIn(privateInput.copy(drinkSlug = "espresso", customDrinkName = "Custom")))
        assertNull(validateShopCheckIn(privateInput.copy(drinkSlug = "other", customDrinkName = "Flat white")))
        val photo = ShopCheckInPhoto(byteArrayOf(1), "visit.jpg")
        assertEquals(ShopCheckInValidationError.TooManyPhotos,
            validateShopCheckIn(privateInput.copy(photos = List(6) { photo })))
    }
}
