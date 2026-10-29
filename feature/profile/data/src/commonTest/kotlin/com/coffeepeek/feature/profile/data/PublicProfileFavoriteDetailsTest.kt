package com.coffeepeek.feature.profile.data

import com.coffeepeek.feature.profile.data.repository.createPublicProfileFavoriteDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterDescription
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterSummary
import com.coffeepeek.feature.shop.api.PublicShopDetails
import com.coffeepeek.feature.shop.api.PublicShopSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicProfileFavoriteDetailsTest {
    @Test fun publicReadPortsRetainAllCatalogAndCardFieldsForTheRequestedEntity() = runTest {
        val f = DetailsFixture()
        val summary = f.details.roasterSummaries().single()
        assertEquals("roast", summary.slug)
        assertEquals(listOf("Светлая"), summary.tags)
        assertEquals(Triple(5, 12, 9), Triple(summary.coffeeShopsCount, summary.coffeeProductsCount, summary.availableCoffeeProducts))
        val shop = f.details.shop("cafe")!!
        assertEquals("cafe", f.shopSlug)
        assertEquals(listOf("Фильтр"), shop.tags)
        assertEquals("card", shop.photoUrl)
        assertEquals("Description", shop.description)
        assertEquals("Address", shop.address)
        assertEquals(4.7, shop.rating)
        assertEquals(12, shop.reviewCount)
        assertTrue(shop.isOpen)
        assertEquals("About", f.details.roaster("roast")?.about)
        assertEquals("roast", f.roasterSlug)
    }

    @Test fun optionalFailuresKeepPlainFavoritesButNeverSwallowCancellation() = runTest {
        val f = DetailsFixture()
        f.failure = IllegalStateException("offline")
        assertTrue(f.details.roasterSummaries().isEmpty())
        assertNull(f.details.shop("cafe"))
        assertNull(f.details.roaster("roast"))
        f.failure = CancellationException("cancelled")
        assertFailsWith<CancellationException> { f.details.roasterSummaries() }
        assertFailsWith<CancellationException> { f.details.shop("cafe") }
        assertFailsWith<CancellationException> { f.details.roaster("roast") }
    }

    private class DetailsFixture {
        var failure: Throwable? = null
        var shopSlug: String? = null
        var roasterSlug: String? = null
        private val shops = object : PublicShopDetails {
            override suspend fun load(slug: String): Result<PublicShopSummary> {
                shopSlug = slug
                return result(PublicShopSummary("card", "Description", "Address", 4.7, 12, true, listOf("Фильтр")))
            }
        }
        private val roasters = object : PublicRoasterDetails {
            override suspend fun summaries() = result(listOf(PublicRoasterSummary("roast", "photo", listOf("Светлая"), 5, 12, 9)))
            override suspend fun load(slug: String): Result<PublicRoasterDescription> {
                roasterSlug = slug
                return result(PublicRoasterDescription("photo", "About"))
            }
        }
        val details = createPublicProfileFavoriteDetails(shops, roasters)
        private fun <T> result(value: T): Result<T> = failure?.let { Result.failure(it) } ?: Result.success(value)
    }
}
