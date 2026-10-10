package com.coffeepeek.feature.shop.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidateShopReviewTest {
    @Test fun trimsBeforeCheckingRequiredAndMinimumLengths() {
        assertEquals(ShopReviewFieldError.Required, validateShopReviewText("   ", "  ").headerError)
        assertEquals(ShopReviewFieldError.Required, validateShopReviewText("   ", "  ").commentError)
        assertEquals(ShopReviewFieldError.TooShort, validateShopReviewText(" ab ", " 123456789 ").headerError)
        assertEquals(ShopReviewFieldError.TooShort, validateShopReviewText(" ab ", " 123456789 ").commentError)
        assertTrue(validateShopReviewText(" abc ", " 1234567890 ").isValid)
    }

    @Test fun rejectsInputBeyondEditorLimits() {
        val validation = validateShopReviewText("h".repeat(121), "c".repeat(2001))
        assertEquals(ShopReviewFieldError.TooLong, validation.headerError)
        assertEquals(ShopReviewFieldError.TooLong, validation.commentError)
        assertTrue(validateShopReviewText("h".repeat(120), "c".repeat(2000)).isValid)
    }
}
