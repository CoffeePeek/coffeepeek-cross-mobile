package com.coffeepeek.admin.utils

import com.coffeepeek.admin.ui.screen.shop.CheckInDraft
import com.coffeepeek.admin.ui.screen.shop.validationError
import com.coffeepeek.admin.utils.PickedImage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CheckInValidationTest {
    @Test
    fun publicAndHiddenCheckInsAcceptDefaultRatingsAndOptionalContent() {
        val valid = CheckInDraft(shopId = "shop", visitMillis = 1000)
        for (isPublic in listOf(false, true)) {
            val visit = valid.copy(isPublic = isPublic)
            assertNull(visit.validationError(now = 2000)) // Drink and photos are optional; no title needed.
            assertNull(visit.copy(note = " \n ").validationError(now = 2000))
            assertNotNull(visit.copy(note = "а".repeat(1001)).validationError(now = 2000))
            assertNotNull(visit.copy(visitMillis = 0).validationError(now = 2000))
            assertNotNull(visit.copy(visitMillis = 2001).validationError(now = 2000))
            assertNotNull(visit.copy(coffeeRating = 0).validationError(now = 2000))
            assertNotNull(visit.copy(serviceRating = 6).validationError(now = 2000))
            assertNotNull(visit.copy(placeRating = 0).validationError(now = 2000))
            assertNotNull(visit.copy(photos = List(6) { PickedImage(byteArrayOf(1), "coffee.jpg") }).validationError(now = 2000))
            assertNotNull(visit.copy(drinkSlug = "other", customDrinkName = " ").validationError(now = 2000))
            assertNotNull(visit.copy(drinkSlug = "other", customDrinkName = "а".repeat(101)).validationError(now = 2000))
            assertNotNull(visit.copy(drinkSlug = "cappuccino", customDrinkName = "Кофе").validationError(now = 2000))
        }
    }

    @Test
    fun acceptsInclusiveLimitsForDateNoteRatingsPhotosAndCustomDrink() {
        val draft = CheckInDraft("shop", visitMillis = 2000, note = "а".repeat(1000),
            coffeeRating = 1, serviceRating = 5, placeRating = 5, drinkSlug = "other", customDrinkName = "а".repeat(100),
            photos = List(5) { PickedImage(byteArrayOf(1), "coffee.jpg") })
        assertNull(draft.validationError(now = 2000))
    }
}
