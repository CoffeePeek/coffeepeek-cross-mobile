package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.checkinhistory.data.repository.createCheckInHistoryRepository
import com.coffeepeek.feature.community.data.repository.createCommunityRepository
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.data.repository.createShopCheckInVoteRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.model.ShopRating
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The check-in, feed and shop-visit fixtures of the Sandbox server, read through the migrated feature clients. */
class SandboxCheckInFixturesTest {
    @get:Rule val temporary = TemporaryFolder()

    private val friend = SandboxApplicationBackend.FRIEND_ID
    private suspend fun json(response: HttpResponse) = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonObject
    private fun history(client: HttpClient) = createCheckInHistoryRepository(client, SandboxBackend.BASE_URL)
    private fun community(client: HttpClient) = createCommunityRepository(client, SandboxBackend.BASE_URL)
    private fun shops(client: HttpClient) = createShopDetailsRepository(client, SandboxBackend.BASE_URL) { 0 }

    @Test fun actualMigratedShopWriteUpdatesHistoryButNeverPublishesPrivateOrPendingRows() = runTest { withSandboxFixture(temporary) { _, client ->
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val feed = community(client.client)
        assertEquals(2, feed.feed(20).getOrThrow().entries.size)
        val input = ShopCheckInCreateInput("sandbox-cafe", "Only local", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z",
            photos = listOf(ShopCheckInPhoto(byteArrayOf(9, 8), "sandbox.png", "image/png")))
        writer.create(input).getOrThrow()
        val visits = history(client.client).page(1, 20).getOrThrow().items
        assertEquals(2, visits.size)
        assertEquals("Only local", visits.last().note)
        assertTrue(visits.last().photoUrls.single().contains("/media/"))
        writer.create(input.copy(visibility = ShopCheckInVisibility.Public)).getOrThrow()
        assertEquals(2, feed.feed(20).getOrThrow().entries.size)
    } }

    @Test fun helpfulVotesDecodeRemainIdempotentAndPersistAcrossReads() = runTest { withSandboxFixture(temporary) { _, client ->
        val history = history(client.client)
        val id = history.page(1, 20).getOrThrow().items.single().id
        val votes = createShopCheckInVoteRepository(client.client)
        val posts = community(client.client)
        assertEquals(HelpfulVote(true, 1), posts.setHelpful(id, true).getOrThrow())
        assertEquals(ShopHelpfulVote(true, 1), votes.setHelpful(id, true).getOrThrow())
        val details = shops(client.client).getDetails("sandbox-cafe").getOrThrow().checkIns.first { it.id == id }
        assertTrue(details.isHelpfulByCurrentUser)
        assertEquals(1, details.helpfulCount)
        assertEquals(ShopHelpfulVote(false, 0), votes.setHelpful(id, false).getOrThrow())
        assertEquals(HelpfulVote(false, 0), posts.setHelpful(id, false).getOrThrow())
        val row = history.page(1, 20).getOrThrow().items.single()
        assertFalse(row.isHelpfulByCurrentUser)
        assertEquals(0, row.helpfulCount)
    } }

    @Test fun createdCheckInsKeepTheirShopAndRejectUnknownShopSlugs() = runTest { withSandboxFixture(temporary) { _, client ->
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val input = ShopCheckInCreateInput("sandbox-lab", "Lab visit", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z")
        writer.create(input).getOrThrow()
        val history = history(client.client)
        val created = history.page(1, 20).getOrThrow().items.last()
        assertEquals("sandbox-lab", created.shop?.slug)
        assertEquals("Sandbox Lab", created.shopName)
        val shops = shops(client.client)
        assertEquals(listOf(created.id), shops.getDetails("sandbox-lab").getOrThrow().userCheckIns.map { it.id })
        assertTrue(shops.getDetails("sandbox-cafe").getOrThrow().userCheckIns.none { it.id == created.id })
        assertTrue(writer.create(input.copy(shopSlug = "unknown-shop")).isFailure)
        assertEquals(2, history.page(1, 20).getOrThrow().items.size)
    } }

    @Test fun publicShopRowsExcludePrivateAndPendingVisitsWhileHistoryRetainsThem() = runTest { withSandboxFixture(temporary) { _, client ->
        val shops = shops(client.client)
        val published = shops.getDetails("sandbox-cafe").getOrThrow().checkIns.map { it.id }
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val input = ShopCheckInCreateInput("sandbox-cafe", "Private visit", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z")
        writer.create(input).getOrThrow()
        writer.create(input.copy(text = "Pending visit", visibility = ShopCheckInVisibility.Public)).getOrThrow()
        val details = shops.getDetails("sandbox-cafe").getOrThrow()
        assertEquals(published, details.checkIns.map { it.id })
        assertEquals(published.size, details.overview.reviewCount)
        assertEquals(3, details.userCheckIns.size)
        assertEquals(3, history(client.client).page(1, 20).getOrThrow().items.size)
        assertEquals(published.size, community(client.client).feed(20).getOrThrow().entries.size)
    } }

    @Test fun sharedPostLinksReadThroughTheRealCommunityRepositoryAndHonourBlocks() = runTest { withSandboxFixture(temporary) { _, client ->
        val posts = createCommunityRepository(client.client, SandboxBackend.BASE_URL)
        val id = SandboxApplicationBackend.FRIEND_VISIT_ID
        assertEquals(friend, posts.postById(id).getOrThrow().author.id)
        assertTrue(posts.postById("sandbox-feed-5").getOrThrow().isHome)
        val own = history(client.client).page(1, 20).getOrThrow().items.single().id
        assertEquals(own, posts.postById(own).getOrThrow().id)
        client.client.put("/api/v1/me/blocks/$friend")
        assertEquals(FailureKind.NotFound, assertIs<CommunityFailure>(posts.postById(id).exceptionOrNull()).kind)
        client.client.delete("/api/v1/me/blocks/$friend")
        assertEquals(id, posts.postById(id).getOrThrow().id)
        assertEquals(FailureKind.NotFound, assertIs<CommunityFailure>(posts.postById("missing").exceptionOrNull()).kind)
    } }

    @Test fun unimplementedMutationsAreRejectedAndEmptyStateIsSharedAcrossSources() = runTest { withSandboxFixture(temporary) { backend, client ->
        assertFalse(client.client.post("/api/ModerationShops").status.isSuccess())
        val history = history(client.client)
        val posts = community(client.client)
        val id = history.page(1, 20).getOrThrow().items.single().id
        assertEquals(HttpStatusCode.MethodNotAllowed, client.client.post("/api/v1/check-ins/$id/helpful").status)
        assertTrue(posts.report(id, "Sandbox report").isFailure)
        assertEquals(1, history.page(1, 20).getOrThrow().items.size)
        assertTrue(shops(client.client).getDetails("unknown-shop").isFailure)
        val discovery = createDiscoveryRepository(client.client, { emptySet() }, { Result.success(emptyList()) })
        backend.reset(false)
        assertTrue(discovery.search(DiscoveryCriteria().searchQuery()).getOrThrow().items.isEmpty())
        assertTrue(posts.feed(20).getOrThrow().entries.isEmpty())
        assertTrue(sandboxAccountFavorites(client).getFavorites(refresh = true).getOrThrow().isEmpty())
    } }

    @Test fun blockingHidesVisitsFromFeedAndShopButKeepsServerAggregatesAndUnblockRestores() = runTest { withSandboxFixture(temporary) { _, client ->
        val feed = community(client.client)
        val shops = shops(client.client)
        suspend fun cards() = shops.getDetails("sandbox-cafe").getOrThrow().checkIns.size
        suspend fun aggregate() = json(client.client.get("/api/coffeeshops/sandbox-cafe"))["checkInCount"]
        assertEquals(2, feed.feed(20).getOrThrow().entries.size); assertEquals(2, cards())
        val count = aggregate()
        client.client.put("/api/v1/me/blocks/$friend")
        assertEquals(1, feed.feed(20).getOrThrow().entries.size); assertEquals(1, cards())
        assertEquals(count, aggregate())
        assertEquals(1, shops.getDetails("sandbox-cafe").getOrThrow().userCheckIns.size)
        assertTrue(json(client.client.get("/api/Users/${SandboxApplicationBackend.FRIEND_SLUG}"))["isBlockedByViewer"]!!.jsonPrimitive.boolean)
        client.client.delete("/api/v1/me/blocks/$friend")
        assertEquals(2, feed.feed(20).getOrThrow().entries.size); assertEquals(2, cards())
    } }

    @Test fun feedFixturesPageThroughTheRealFeedServiceAndHonourBlocksAndFailures() = runTest { withSandboxFixture(temporary) { backend, client ->
        val feed = community(client.client)
        // Default: the visible fixtures in one page.
        assertEquals(2, feed.feed(20).getOrThrow().entries.size)
        assertNull(feed.feed(20).getOrThrow().nextCursor)

        backend.feed = SandboxFeed.Paged
        val first = feed.feed(20).getOrThrow()
        assertEquals(20, first.entries.size); assertEquals("20", first.nextCursor)
        val second = feed.feed(20, first.nextCursor).getOrThrow()
        val third = feed.feed(20, second.nextCursor).getOrThrow()
        val all = first.entries + second.entries + third.entries
        val ids = all.map { it.post.id }
        assertEquals(47, ids.size); assertEquals(ids.size, ids.toSet().size); assertNull(third.nextCursor)
        assertTrue(all.any { it.post.isHome && it.post.home?.beanName != null })
        // A blocked author disappears from every page and returns on unblock.
        client.client.put("/api/v1/me/blocks/$friend")
        val visible = feed.feed(100).getOrThrow().entries
        assertEquals(47 - 1 - 15, visible.size); assertTrue(visible.none { it.post.author.id == friend })
        client.client.delete("/api/v1/me/blocks/$friend")
        assertEquals(47, feed.feed(100).getOrThrow().entries.size)
        // Votes work for generated posts too and keep their state.
        assertEquals(HelpfulVote(true, 2), feed.setHelpful("sandbox-feed-1", true).getOrThrow())
        assertEquals(1, feed.setHelpful("sandbox-feed-1", false).getOrThrow().helpfulCount)

        backend.feed = SandboxFeed.PageFailsOnce
        val firstPage = feed.feed(20).getOrThrow()
        assertTrue(feed.feed(20, firstPage.nextCursor).isFailure)
        assertEquals(20, feed.feed(20, firstPage.nextCursor).getOrThrow().entries.size)

        backend.feed = SandboxFeed.InvalidCursor
        assertEquals(20, feed.feed(20).getOrThrow().entries.size)
        val restart = feed.feed(20, "20").exceptionOrNull()
        assertTrue(assertIs<FeedLoadException>(restart).restartPagination)

        backend.feed = SandboxFeed.Empty
        assertTrue(feed.feed(20).getOrThrow().entries.isEmpty())
        backend.feed = SandboxFeed.Failure
        assertTrue(feed.feed(20).isFailure)

        backend.feed = SandboxFeed.Paged
        backend.reset(true)
        assertEquals(SandboxFeed.Single, backend.feed)
    } }
}
