package com.coffeepeek.admin.feature.profile.data

import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class LegacyPublicProfileFavoriteDetailsTest {
    @Test
    fun legacyFavoriteDetailsMapCatalogSummariesAndTreatFailuresAsNoExtraData(): Unit = runBlocking {
        var failCatalog = false
        val roasters = object : RoasterRepository {
            override suspend fun getRoasters(): Result<List<RoasterSummary>> =
                if (failCatalog) Result.failure(IllegalStateException("Catalog unavailable"))
                else Result.success(listOf(
                    RoasterSummary(address("first"), "Catalog name", tags = listOf(CatalogItem("light", "Светлая", "light")), coffeeShopsCount = 5, coffeeProductsCount = 12, availableCoffeeProducts = 9),
                    RoasterSummary(null, "No address", coffeeShopsCount = 1),
                ))
            override suspend fun getRoaster(id: String): Result<RoasterDetails> = Result.failure(IllegalStateException("Details unavailable"))
            override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("No mutation expected")
            override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("No private data expected")
        }
        val details = LegacyPublicProfileFavoriteDetails(shops = null, roasters = roasters)
        val summary = details.roasterSummaries().single()
        assertEquals("first", summary.slug)
        assertEquals(listOf("Светлая"), summary.tags)
        assertEquals(Triple(5, 12, 9), Triple(summary.coffeeShopsCount, summary.coffeeProductsCount, summary.availableCoffeeProducts))
        assertNull(details.roaster("first"))
        assertNull(details.shop("any"))
        failCatalog = true
        assertTrue(details.roasterSummaries().isEmpty())
    }

    private fun address(slug: String) = PublicAddress(slug, "/users/$slug", 1, false)
}
