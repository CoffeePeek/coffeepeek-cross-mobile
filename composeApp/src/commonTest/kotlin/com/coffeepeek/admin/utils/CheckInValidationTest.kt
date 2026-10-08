package com.coffeepeek.admin.utils

import com.coffeepeek.admin.ui.screen.shop.CheckInDraft
import com.coffeepeek.admin.ui.screen.shop.validationError
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CheckInValidationTest {
    @Test
    fun publicAndHiddenCheckInsRequireTheSameCompleteVisit() {
        val valid = CheckInDraft(shopId = "shop", visitMillis = 1000, note = " Хороший кофе ")
        for (isPublic in listOf(false, true)) {
            val visit = valid.copy(isPublic = isPublic)
            assertNull(visit.validationError(now = 2000)) // Drink and photos are optional; no title needed.
            assertNotNull(visit.copy(note = " \n ").validationError(now = 2000))
            assertNotNull(visit.copy(note = "а".repeat(1001)).validationError(now = 2000))
            assertNotNull(visit.copy(visitMillis = 0).validationError(now = 2000))
            assertNotNull(visit.copy(visitMillis = 2001).validationError(now = 2000))
            assertNotNull(visit.copy(coffeeRating = 0).validationError(now = 2000))
            assertNotNull(visit.copy(serviceRating = 6).validationError(now = 2000))
            assertNotNull(visit.copy(placeRating = 0).validationError(now = 2000))
        }
    }
}
