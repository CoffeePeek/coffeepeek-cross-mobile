package com.coffeepeek.admin.di.discovery

import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LegacyDiscoveryMapSourceTest {
    @Test fun adapterForwardsCriteriaAndMapsAllCanvasContent() = runBlocking {
        val repository = MapRepository()
        val source = LegacyDiscoveryMapSource(repository)
        val result = source.load(MapBounds(53.8, 27.4, 54.0, 27.7), 14f,
            ShopFilters(query = "coffee", tagIds = listOf("laptop-friendly"), favoritesOnly = true)).getOrThrow()
        assertEquals("coffee", repository.criteria?.text)
        assertTrue(repository.criteria!!.filters.favoritesOnly)
        assertEquals(setOf("laptop-friendly"), repository.criteria!!.filters.tagIds)
        assertEquals("shop", result.shops.single().id)
        assertEquals("specialty", result.shops.single().type)
        assertEquals("zone", result.shops.single().primaryZoneId)
        assertEquals(3, result.clusters.single().count)
        assertEquals(2, result.zones.single().polygon.size)
        assertTrue(result.isTruncated)
    }

    @Test fun endpointFailureIsReturnedWithoutAnUnfilteredFallback() = runBlocking {
        val error = IllegalStateException("offline")
        val repository = MapRepository().apply { failure = error }
        assertSame(error, LegacyDiscoveryMapSource(repository).load(MapBounds(0.0, 0.0, 1.0, 1.0), 10f, ShopFilters()).exceptionOrNull())
        assertEquals(1, repository.calls)
    }
}

private class MapRepository : DiscoveryRepository {
    var criteria: DiscoveryCriteria? = null
    var failure: Throwable? = null
    var calls = 0
    override suspend fun mapContent(bounds: com.coffeepeek.feature.discovery.domain.model.MapBounds, zoom: Float, criteria: DiscoveryCriteria): Result<MapContent> {
        this.criteria = criteria
        calls++
        failure?.let { return Result.failure(it) }
        val point = Coordinates(53.9, 27.56)
        return Result.success(MapContent(markers = listOf(MapMarker("shop", "Shop", point, ShopKind.Specialty, "zone")),
            clusters = listOf(MapCluster("cluster", point, 3, bounds)),
            zones = listOf(MapZone("zone", "Zone", "", null, point, 300.0, 2, listOf(point, point))), isTruncated = true))
    }
    override suspend fun search(query: ShopQuery): Result<Paged<ShopSummary>> = error("Map adapter must not call search")
    override suspend fun catalogs(): Result<DiscoveryCatalogs> = error("Map adapter must not read catalogs")
}
