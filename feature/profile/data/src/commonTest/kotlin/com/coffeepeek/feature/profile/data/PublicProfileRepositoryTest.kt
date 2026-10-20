package com.coffeepeek.feature.profile.data

import com.coffeepeek.feature.profile.domain.*
import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PublicProfileRepositoryTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun usesNewPublicProfileEndpointAndAllServerCountsWithoutFixtureSections(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/Users/old-nick", request.url.encodedPath)
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            respond("""{"data":{"userName":"new_nick","about":"Кофе каждый день","avatarUrl":"/api/Photos/avatar.jpg","checkInCount":0,"addedShopsCount":4,"addedRoastersCount":2,"followersCount":17,"isFollowing":true},"address":{"slug":"new-nick","canonicalPath":"/users/new-nick","revision":2,"isAlias":true}}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val profile = PublicProfileRepositoryImpl(client).getProfile("old-nick").getOrThrow()
            assertEquals("new_nick", profile.nickname)
            assertEquals("new-nick", profile.slug)
            assertEquals("/users/new-nick", profile.canonicalPath)
            assertEquals("https://api.coffeepeek.by/api/Photos/avatar.jpg", profile.avatarUrl)
            assertEquals(PublicProfileStats(0, 4, 2, 17), profile.stats)
            assertTrue(profile.isFollowing)
            assertTrue(profile.favorites.isEmpty())
            assertNull(profile.activity)
        } finally { client.close() }
    }

    @Test
    fun followAndUnfollowUseDirectServerStateWithoutCountingLocally(): Unit = runBlocking {
        val methods = mutableListOf<HttpMethod>()
        val client = HttpClientFactory(MockEngine { request ->
            methods += request.method
            assertEquals("/api/Users/coffee%20%2F%20friend/follow", request.url.encodedPath)
            respond(if (request.method == HttpMethod.Put) """{"followersCount":44,"isFollowing":true}""" else """{"followersCount":3,"isFollowing":false}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val repo = PublicProfileRepositoryImpl(client)
            assertEquals(PublicProfileFollowState(true, 44), repo.setFollowing("coffee / friend", true).getOrThrow())
            assertEquals(PublicProfileFollowState(true, 44), repo.setFollowing("coffee / friend", true).getOrThrow())
            assertEquals(PublicProfileFollowState(false, 3), repo.setFollowing("coffee / friend", false).getOrThrow())
            assertEquals(listOf(HttpMethod.Put, HttpMethod.Put, HttpMethod.Delete), methods)
        } finally { client.close() }
    }

    @Test
    fun loadsOtherUsersPublicFavoritesWithAllKindsNamesImagesAndRealAddresses(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/v1/profiles/glasha/favorites", request.url.encodedPath)
            respond("""{"isSuccess":true,"data":[
                {"kind":"coffee_shop","name":"Территория кофе","createdAtUtc":"2026-10-15T10:00:00Z","coverPhoto":{"fullUrl":"/photo/full.jpg","urls":{"card":"/photo/card.jpg"}},"address":{"slug":"coffee-place","canonicalPath":"/shops/coffee-place","revision":3,"isAlias":false}},
                {"kind":"roaster","name":"Memories","createdAtUtc":"2026-10-14T10:00:00Z","coverPhoto":null,"address":{"slug":"memories","canonicalPath":"/roasters/memories","revision":1,"isAlias":false}},
                {"kind":"drink","name":"Матча","createdAtUtc":"2026-10-13T10:00:00Z","coverPhoto":{"fullUrl":"/photo/drink.jpg","urls":null},"drink":{"slug":"matcha"}}
            ]}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val items = PublicProfileRepositoryImpl(client).getFavorites("glasha").getOrThrow()
            assertEquals(listOf(PublicFavoriteKind.CoffeeShop, PublicFavoriteKind.Roaster, PublicFavoriteKind.Drink), items.map { it.kind })
            assertEquals(listOf("Территория кофе", "Memories", "Матча"), items.map { it.title })
            assertEquals("https://api.coffeepeek.by/photo/card.jpg", items[0].imageUrl)
            assertEquals("/shops/coffee-place", items[0].publicAddress?.canonicalPath)
            assertEquals(3, items[0].publicAddress?.revision)
            assertNull(items[0].isOpen)
            assertNull(items[1].imageUrl)
            assertEquals("matcha", items[2].drinkSlug)
            assertNull(items[2].publicAddress)
            assertEquals("https://api.coffeepeek.by/photo/drink.jpg", items[2].imageUrl)
        } finally { client.close() }
    }

    @Test
    fun loadsRequestedActivityMonthAndTimeZoneKeepingZeroDaysAndActualCounts(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/v1/profiles/glasha/activity", request.url.encodedPath)
            assertEquals("2026", request.url.parameters["year"])
            assertEquals("10", request.url.parameters["month"])
            assertEquals("Europe/Minsk", request.url.parameters["timeZone"])
            respond("""{"isSuccess":true,"data":{"year":2026,"month":10,"timeZone":"Europe/Minsk","activeDays":1,"totalCheckIns":3,"days":[{"date":"2026-10-01","checkInCount":0},{"date":"2026-10-02","checkInCount":3}]}}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val activity = PublicProfileRepositoryImpl(client).getActivity("glasha", 2026, 10).getOrThrow()
            assertEquals("2026-10", activity.month)
            assertEquals("Europe/Minsk", activity.timeZone)
            assertEquals(setOf(2), activity.activeDays)
            assertEquals(mapOf(1 to 0, 2 to 3), activity.checkInCounts)
            assertEquals(3, activity.totalCheckIns)
        } finally { client.close() }
    }

    @Test
    fun retainsDistinctUnaddressedFavoritesWithIdenticalNamesAndTimestampsAsReadOnly(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine {
            respond("""{"isSuccess":true,"data":[
                {"kind":"coffee_shop","name":"Кофейня","createdAtUtc":"2026-10-15T10:00:00Z","address":null,"coverPhoto":{"fullUrl":"/first.jpg"}},
                {"kind":"coffee_shop","name":"Кофейня","createdAtUtc":"2026-10-15T10:00:00Z","address":null,"coverPhoto":{"fullUrl":"/second.jpg"}}
            ]}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val entries = PublicProfileRepositoryImpl(client).getFavorites("glasha").getOrThrow()
            assertEquals(2, entries.size)
            assertEquals(2, entries.map { it.id }.toSet().size)
            assertTrue(entries.all { it.publicAddress == null && it.slug == null })
            assertEquals(listOf("https://api.coffeepeek.by/first.jpg", "https://api.coffeepeek.by/second.jpg"), entries.map { it.imageUrl })
        } finally { client.close() }
    }

    @Test
    fun roasterCatalogLoadsOnceAndDetailFailuresPreserveEntriesAndKnownCounts(): Unit = runBlocking {
        var catalogRequests = 0
        var failCatalog = false
        val details = object : PublicProfileFavoriteDetails {
            override suspend fun roasterSummaries(): List<FavoriteRoasterSummary> {
                catalogRequests++
                if (failCatalog) throw IllegalStateException("Catalog unavailable")
                return listOf(
                    FavoriteRoasterSummary("first", null, listOf("Светлая"), 5, 12, 9),
                    FavoriteRoasterSummary("second", null, emptyList(), 2, 4, 3),
                )
            }
            override suspend fun shop(slug: String): FavoriteShopDetails? = null
            override suspend fun roaster(slug: String): FavoriteRoasterDetails? = throw IllegalStateException("Details unavailable")
        }
        val client = HttpClientFactory(MockEngine {
            respond("""{"isSuccess":true,"data":[
                {"kind":"roaster","name":"First saved","createdAtUtc":"2026-10-15","address":{"slug":"first","canonicalPath":"/roasters/first"}},
                {"kind":"roaster","name":"Second saved","createdAtUtc":"2026-10-14","address":{"slug":"second","canonicalPath":"/roasters/second"}}
            ]}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val repo = PublicProfileRepositoryImpl(client, details)
            val entries = repo.getFavorites("glasha").getOrThrow()
            assertEquals(1, catalogRequests)
            assertEquals(listOf("First saved", "Second saved"), entries.map { it.title })
            assertEquals<List<Int?>>(listOf(5, 2), entries.map { it.coffeeShopsCount })
            assertEquals(12, entries[0].coffeeProductsCount)
            assertEquals(9, entries[0].availableCoffeeProducts)
            assertEquals(listOf("Светлая"), entries[0].tags)
            failCatalog = true
            val unavailable = repo.getFavorites("glasha").getOrThrow()
            assertEquals(2, catalogRequests)
            assertEquals(listOf("First saved", "Second saved"), unavailable.map { it.title })
            assertTrue(unavailable.all { it.coffeeShopsCount == null && it.coffeeProductsCount == null && it.availableCoffeeProducts == null })
        } finally { client.close() }
    }
    @Test
    fun connectionsUseDirectPagedBodyAndKeepServerNicknamesAndAddresses(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            assertTrue(request.url.encodedPath == "/api/Users/glasha/followers" || request.url.encodedPath == "/api/Users/glasha/following")
            assertEquals("2", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["pageSize"])
            respond("""{"items":[{"userName":"petr","avatarUrl":"/avatar/petr.jpg","address":{"slug":"petr-public","canonicalPath":"/users/petr-public","revision":1,"isAlias":false}}],"totalCount":21,"page":2,"pageSize":20}""", headers = jsonHeaders)
        }).api("https://api.coffeepeek.by")
        try {
            val repo = PublicProfileRepositoryImpl(client)
            for (kind in PublicProfileConnectionKind.entries) {
                val page = repo.getConnections("glasha", kind, 2, 20).getOrThrow()
                assertEquals(21, page.totalCount)
                assertEquals(2, page.page)
                assertEquals("petr", page.items.single().userName)
                assertEquals("petr-public", page.items.single().slug)
                assertEquals("https://api.coffeepeek.by/avatar/petr.jpg", page.items.single().avatarUrl)
            }
        } finally { client.close() }
    }

    @Test
    fun failuresRemainErrorsAndInvalidParametersNeverRequestFallbackData(): Unit = runBlocking {
        var requests = 0
        val client = HttpClientFactory(MockEngine {
            requests++
            respond("", HttpStatusCode.Unauthorized)
        }).api("https://api.coffeepeek.by")
        try {
            val repo = PublicProfileRepositoryImpl(client)
            assertTrue(repo.setFollowing("glasha", true).exceptionOrNull() is PublicProfileAuthenticationRequired)
            assertTrue(repo.getProfile("").isFailure)
            assertTrue(repo.getActivity("glasha", 2026, 13).isFailure)
            assertTrue(repo.getActivity("glasha", 1999, 12).isFailure)
            assertTrue(repo.getActivity("glasha", 2101, 1).isFailure)
            assertTrue(repo.getConnections("glasha", PublicProfileConnectionKind.Followers, 0, 20).isFailure)
            assertEquals(1, requests)
        } finally { client.close() }
    }


    @Test
    fun preservesRequestCancellation(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { throw CancellationException("Profile closed") }).api("https://api.coffeepeek.by")
        try { assertFailsWith<CancellationException> { PublicProfileRepositoryImpl(client).getProfile("glasha") } }
        finally { client.close() }
    }

    @Test
    fun unavailableOrDeletedProfileRemainsTypedFailure(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { respond("", HttpStatusCode.NotFound) }).api("https://api.coffeepeek.by")
        try { assertTrue(PublicProfileRepositoryImpl(client).getProfile("deleted").exceptionOrNull() is PublicProfileUnavailable) }
        finally { client.close() }
    }

}
