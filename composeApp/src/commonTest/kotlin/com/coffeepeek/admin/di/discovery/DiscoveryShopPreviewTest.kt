package com.coffeepeek.admin.di.discovery

import com.coffeepeek.feature.discovery.api.MapOpeningHours
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.shop.api.PublicShopCoordinates
import com.coffeepeek.feature.shop.api.PublicShopOpeningHours
import com.coffeepeek.feature.shop.api.PublicShopPreview
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class DiscoveryShopPreviewTest {
    private val row = PublicShopPreview("card", "detail", 4.7, 12, true,
        PublicShopOpeningHours(false, "09:00", "23:00"), PublicShopCoordinates(53.9, 27.5))

    @Test fun mapReadsPublicCoverHoursAndCoordinatesWithoutARepositoryBridge() = runTest {
        var requested: String? = null
        val adapter = ShopDiscoveryDetails(PublicShopPreviews { requested = it; Result.success(row) })
        val details = adapter.details("cafe").getOrThrow()
        assertEquals("cafe", requested)
        assertEquals("card", details.photoUrl)
        assertEquals(4.7, details.rating)
        assertEquals(12, details.reviewCount)
        assertEquals(MapOpeningHours(false, "09:00", "23:00"), details.hours)
        assertEquals(Coordinates(53.9, 27.5), details.coordinates)
    }

    @Test fun invalidCoordinatesCannotMoveTheMapAndAbsentHoursStayAbsent() = runTest {
        for (latitude in listOf(91.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val adapter = ShopDiscoveryDetails(PublicShopPreviews {
                Result.success(row.copy(hours = null, coordinates = PublicShopCoordinates(latitude, 27.5)))
            })
            val details = adapter.details("cafe").getOrThrow()
            assertNull(details.coordinates)
            assertNull(details.hours)
        }
    }

    @Test fun failuresAndCancellationPreserveThePublicReadContract() = runTest {
        val error = IllegalStateException("offline")
        assertSame(error, ShopDiscoveryDetails(PublicShopPreviews { Result.failure(error) }).details("cafe").exceptionOrNull())
        assertFailsWith<CancellationException> {
            ShopDiscoveryDetails(PublicShopPreviews { throw CancellationException("cancel") }).details("cafe")
        }
    }
}
