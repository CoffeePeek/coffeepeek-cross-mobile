package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import io.ktor.client.request.delete
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SandboxDiscoveryMapTest {
    private val bounds = MapBounds(51.0, 22.0, 55.0, 29.0)

    private fun repository(backend: SandboxBackend): DiscoveryRepository =
        createDiscoveryRepository(backend.gateway, { emptySet() }, { Result.success(emptyList()) })

    @Test fun seededAndPagedMapUseTheSameSelectionAsSearch() = runTest {
        val backend = SandboxBackend(byteArrayOf(1))
        try {
            val repository = repository(backend)
            for (mode in listOf(SandboxDiscovery.Seeded, SandboxDiscovery.Paged)) {
                backend.discovery = mode
                for (criteria in selections()) {
                    val list = repository.search(criteria.searchQuery(pageSize = 100)).getOrThrow()
                    val map = repository.mapContent(bounds, 22f, criteria).getOrThrow()
                    assertEquals(list.items.map { it.id }.toSet(), map.markers.map { it.id }.toSet(), "$mode $criteria")
                }
            }
        } finally { backend.close() }
    }

    private fun selections(): List<DiscoveryCriteria> = listOf(
        DiscoveryFilters(), DiscoveryFilters(openOnly = true), DiscoveryFilters(newOnly = true),
        DiscoveryFilters(visitedOnly = true), DiscoveryFilters(favoritesOnly = true),
        DiscoveryFilters(cityId = "brest"), DiscoveryFilters(kind = ShopKind.Specialty),
        DiscoveryFilters(tagIds = setOf("wifi", "pets")), DiscoveryFilters(roasterIds = setOf("sandbox-light")),
        DiscoveryFilters(beanIds = setOf("arabica")), DiscoveryFilters(equipmentIds = setOf("grinder")),
        DiscoveryFilters(brewMethodIds = setOf("aeropress")), DiscoveryFilters(minRating = 4.2),
        DiscoveryFilters(priceLevel = PriceLevel.Cheap), DiscoveryFilters(nearbyOnly = true, cityId = "minsk"),
    ).map { DiscoveryCriteria(filters = it, origin = Coordinates(52.0976, 23.7341)) } +
        DiscoveryCriteria("Lab", DiscoveryFilters(tagIds = setOf("wifi")))

    @Test fun mapFavoritesRefreshAndReadEndpointWorksDuringSaveErrors() = runTest {
        val backend = SandboxBackend(byteArrayOf(1))
        try {
            val repository = repository(backend)
            val criteria = DiscoveryCriteria(filters = DiscoveryFilters(favoritesOnly = true))
            assertEquals(listOf("sandbox-cafe"), repository.mapContent(bounds, 22f, criteria).getOrThrow().markers.map { it.id })
            backend.gateway.delete("/api/v1/favorites/coffee_shop/sandbox-cafe")
            backend.scenario = SandboxScenario.SaveError
            assertTrue(repository.mapContent(bounds, 22f, criteria).getOrThrow().markers.isEmpty())
        } finally { backend.close() }
    }

    @Test fun clustersAndZonesCountOnlyMatchingShopsAndMapFailuresRemainFailures() = runTest {
        val backend = SandboxBackend(byteArrayOf(1))
        try {
            backend.discovery = SandboxDiscovery.Paged
            val repository = repository(backend)
            val criteria = DiscoveryCriteria(filters = DiscoveryFilters(openOnly = true, cityId = "minsk"))
            val total = repository.search(criteria.searchQuery(pageSize = 100)).getOrThrow().totalCount
            val content = repository.mapContent(bounds, 12f, criteria).getOrThrow()
            assertEquals(total, content.markers.size + content.clusters.sumOf { it.count } + content.zones.sumOf { it.shopCount })
            backend.discovery = SandboxDiscovery.MapFailure
            assertTrue(repository.mapContent(bounds, 22f, criteria).isFailure)
        } finally { backend.close() }
    }
}
