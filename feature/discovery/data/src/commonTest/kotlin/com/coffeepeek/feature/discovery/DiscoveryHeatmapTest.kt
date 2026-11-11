package com.coffeepeek.feature.discovery

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class DiscoveryHeatmapTest {
    private val requests = mutableListOf<Pair<String, JsonObject>>()
    private var mapResponse = """{"shops":[{"address":{"slug":"a"},"latitude":53.9,"longitude":27.56}]}"""
    private var pageResponse: (Int) -> String = { page(it, 1, "a" to 10) }
    private var searchStatus = HttpStatusCode.OK
    private var cancelSearch = false
    private val bounds = MapBounds(53.8, 27.4, 54.0, 27.7)
    private val criteria = DiscoveryCriteria(filters = DiscoveryFilters(cityId = "minsk"))
    private val client = HttpClientFactory(MockEngine { request ->
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        val map = request.url.encodedPath.endsWith("/map")
        requests += request.url.encodedPath to body
        if (!map && cancelSearch) throw CancellationException("cancelled")
        val data = if (map) mapResponse else pageResponse(body.getValue("page").jsonPrimitive.int)
        respond("""{"isSuccess":true,"data":$data}""", if (map) HttpStatusCode.OK else searchStatus,
            headersOf(HttpHeaders.ContentType, "application/json"))
    }).api("https://api.example")
    private val repository = createDiscoveryRepository(client, { emptySet() }, { Result.success(emptyList()) })

    @AfterTest fun close() = client.close()

    @Test fun loadsAllPagesAndJoinsBySlugWithoutDoubleCounting() = runBlocking {
        mapResponse = """{"shops":[
            {"address":{"slug":"a"},"latitude":53.9,"longitude":27.56},
            {"address":{"slug":"a"},"latitude":53.9,"longitude":27.56},
            {"address":{"slug":"b"},"latitude":53.91,"longitude":27.57}]}"""
        pageResponse = { if (it == 1) page(1, 2, "b" to 4) else page(2, 2, "a" to 40) }
        val result = repository.checkInHeatmap(bounds, criteria).getOrThrow()
        assertEquals(listOf("a", "b"), result.points.map { it.shopId })
        assertEquals(listOf(40, 4), result.points.map { it.checkInCount })
        assertFalse(result.isTruncated)
        assertEquals(3, requests.size)
    }

    @Test fun mapAndCountersUseTheSameFiltersAndHeatmapAlwaysRequestsIndividualPoints() = runBlocking {
        val selection = DiscoveryCriteria("  coffee  ", DiscoveryFilters(cityId = "minsk", openOnly = true,
            favoritesOnly = true, kind = ShopKind.Specialty, roasterIds = setOf("roaster")))
        repository.checkInHeatmap(bounds, selection).getOrThrow()
        val (map, search) = requests.map { it.second }
        assertEquals(22, map.getValue("zoom").jsonPrimitive.int)
        assertEquals(map["filters"], search["filters"])
        assertEquals("coffee", search.getValue("q").jsonPrimitive.content)
        assertEquals(100, search.getValue("pageSize").jsonPrimitive.int)
        assertEquals(53.8, map.getValue("bounds").jsonObject.getValue("minLat").jsonPrimitive.double)
    }

    @Test fun zeroNegativeAndInvalidCoordinatesNeverCreateHeat() = runBlocking {
        mapResponse = """{"shops":[
            {"address":{"slug":"a"},"latitude":53.9,"longitude":27.56},
            {"address":{"slug":"b"},"latitude":53.9,"longitude":27.56},
            {"address":{"slug":"bad"},"latitude":91.0,"longitude":27.56}]}"""
        pageResponse = { page(1, 1, "a" to 0, "b" to -2, "bad" to 80) }
        val result = repository.checkInHeatmap(bounds, criteria).getOrThrow()
        assertTrue(result.points.isEmpty())
        assertTrue(result.isTruncated)
    }

    @Test fun anUnmatchedShopIsPartialDataRatherThanZeroVisits() = runBlocking {
        pageResponse = { page(1, 1, "other" to 20) }
        val result = repository.checkInHeatmap(bounds, criteria).getOrThrow()
        assertTrue(result.points.isEmpty())
        assertTrue(result.isTruncated)
    }

    @Test fun anEmptyMapDoesNotFetchTheCatalogAndKeepsTruncation() = runBlocking {
        mapResponse = """{"shops":[],"isTruncated":true}"""
        val result = repository.checkInHeatmap(bounds, criteria).getOrThrow()
        assertTrue(result.isTruncated)
        assertEquals(1, requests.size)
    }

    @Test fun panningReusesTheCompletedCountSnapshotButChangingFiltersDoesNot() = runBlocking {
        repository.checkInHeatmap(bounds, criteria).getOrThrow()
        repository.checkInHeatmap(bounds.copy(minLat = 53.81), criteria).getOrThrow()
        assertEquals(1, requests.count { it.first.endsWith("/search") })
        repository.checkInHeatmap(bounds, criteria.copy(text = "other")).getOrThrow()
        assertEquals(2, requests.count { it.first.endsWith("/search") })
    }

    @Test fun personalizedSelectionsNeverReuseCountSnapshots() = runBlocking {
        for (filters in listOf(DiscoveryFilters(visitedOnly = true), DiscoveryFilters(favoritesOnly = true))) {
            val selection = DiscoveryCriteria(filters = filters)
            repository.checkInHeatmap(bounds, selection).getOrThrow()
            repository.checkInHeatmap(bounds, selection).getOrThrow()
        }
        assertEquals(4, requests.count { it.first.endsWith("/search") })
    }

    @Test fun failedPagesAreNotCachedAndARequestCanBeRetried() = runBlocking {
        searchStatus = HttpStatusCode.ServiceUnavailable
        assertTrue(repository.checkInHeatmap(bounds, criteria).isFailure)
        searchStatus = HttpStatusCode.OK
        assertEquals(10, repository.checkInHeatmap(bounds, criteria).getOrThrow().points.single().checkInCount)
    }

    @Test fun repeatedPageNumberFailsInsteadOfLoopingOrPresentingAnIncompleteSnapshot() = runBlocking {
        pageResponse = { page(1, 2, "a" to 10) }
        assertTrue(repository.checkInHeatmap(bounds, criteria).isFailure)
        assertEquals(3, requests.size)
    }

    @Test fun cancellationIsRethrown() = runBlocking<Unit> {
        cancelSearch = true
        assertFailsWith<CancellationException> { repository.checkInHeatmap(bounds, criteria) }
    }

    private fun page(number: Int, pages: Int, vararg counts: Pair<String, Int>): String {
        val items = counts.joinToString { (id, count) -> """{"address":{"slug":"$id"},"checkInCount":$count}""" }
        return """{"items":[$items],"currentPage":$number,"totalPages":$pages}"""
    }
}
