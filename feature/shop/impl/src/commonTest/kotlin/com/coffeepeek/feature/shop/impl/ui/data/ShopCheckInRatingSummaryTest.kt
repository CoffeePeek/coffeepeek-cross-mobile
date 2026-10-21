package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopCheckInRatingSummaryTest {
    @Test fun emptyOrUnratedSampleHasNoSummary() {
        assertNull(emptyList<ShopCheckIn>().ratingSummary())
        assertNull(listOf(visit(null)).ratingSummary())
    }

    @Test fun averagesUseRatedVisitsOnlyAndKeepCategoriesSeparate() {
        val visits = listOf(visit(ShopRating(place = 3, service = 4, coffee = 5)),
            visit(ShopRating(place = 5, service = 2, coffee = 3)).copy(id = "second"),
            visit(null).copy(id = "unrated"))
        val summary = visits.ratingSummary()!!
        assertEquals(4.0, summary.coffee)
        assertEquals(3.0, summary.service)
        assertEquals(4.0, summary.place)
        assertEquals("3.7", formatShopRating(summary.overall))
    }

    @Test fun ratingFormatKeepsOneDecimalLikeTheCurrentScreen() {
        assertEquals("5.0", formatShopRating(5.0))
        assertEquals("4.7", formatShopRating(14.0 / 3.0))
        assertEquals("0.0", formatShopRating(0.0))
    }

    private fun visit(rating: ShopRating?) = ShopCheckIn(
        id = "visit", userId = "user", shopId = "shop", note = "", createdAt = "", visitedAt = "",
        reviewId = null, photoUrls = emptyList(), photoThumbnailUrls = emptyList(), rating = rating,
    )
}
