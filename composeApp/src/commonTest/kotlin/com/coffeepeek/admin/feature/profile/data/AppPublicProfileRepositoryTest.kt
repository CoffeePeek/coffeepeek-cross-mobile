package com.coffeepeek.admin.feature.profile.data

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import com.coffeepeek.domain.feature.feed.*
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.feature.profile.data.createPublicProfileData
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class AppPublicProfileRepositoryTest {
    @Test
    fun checkInsStayFilteredByAuthorVisibilityAndModerationAndRetainHomeRecipe(): Unit = runBlocking {
        val shop = CheckIn(
            "shop", "coffee-shop", "Кофейня", "Заметка", "2026-10-14",
            authorAddress = address("glasha"), shopAddress = address("coffee-shop"),
            visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Approved,
            username = "glasha", authorAvatarUrl = "https://images.test/avatar.jpg",
            rating = ReviewRating(5, 4, 3), drinkNameRu = "Флэт уайт", drinkNameEn = "Flat white",
            photoUrls = listOf("https://images.test/full.jpg"), photoThumbnailUrls = listOf("https://images.test/thumb.jpg"),
            helpfulCount = 3, isHelpfulByCurrentUser = true,
        )
        val home = shop.copy(id = "home", isHome = true,
            homePreparation = FeedHomePreparation("2026-10-12", "hario-v60", "Колумбия", 15.0, 120, 5))
        val feed = object : FeedRepository {
            override suspend fun getFeed(pageSize: Int, cursor: String?, filters: FeedFilters): Result<FeedPage> {
                assertEquals(FeedFilters(authorSlug = "glasha"), filters)
                assertEquals("cursor", cursor)
                return Result.success(FeedPage(listOf(shop, shop.copy(id = "private", visibility = CheckInVisibility.Private), shop.copy(id = "other", authorAddress = address("other")), shop.copy(id = "pending", moderationState = CheckInModerationState.Pending), home).map { FeedItem("2026-10-15", it) }, "next"))
            }
        }
        val client = HttpClientFactory(MockEngine { error("No private/profile endpoint expected") }).api("https://api.coffeepeek.by")
        try {
            val page = AppPublicProfileRepository(createPublicProfileData(client), feed).getCheckIns("glasha", "cursor").getOrThrow()
            assertEquals(listOf("shop", "home"), page.items.map { it.id })
            assertEquals("next", page.nextCursor)
            assertEquals("Колумбия", page.items.last().title)
            assertEquals(5.0, page.items.last().rating)
            assertNull(page.items.last().coffeeShopSlug)
            assertSame(shop, page.items.first().source)
            assertSame(home, page.items.last().source)
        } finally { client.close() }
    }

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
