package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.contributions.data.repository.createContributionCatalogSource
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.ShopQuery
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore
import com.coffeepeek.feature.roaster.data.repository.createRoasterRepository
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxDiscoveryFixturesTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun discovery(client: ApiClients): DiscoveryRepository =
        createDiscoveryRepository(client.client, { emptySet() }, { Result.success(emptyList()) })

    /** The catalog source keeps a successful read, so a scenario change needs a new one. */
    private fun catalogs(client: ApiClients) = createContributionCatalogSource(client.client) { Result.success(emptyList()) }

    private fun query(
        text: String? = null,
        filters: DiscoveryFilters = DiscoveryFilters(),
        origin: Coordinates? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ) = ShopQuery(text, filters, origin, page, pageSize)

    @Test fun allFourTransportsAreMockOnlyAndRealServicesDecodeFixtures() = runTest { withSandboxFixture(temporary) { backend, client ->
        listOf(client.client, client.plainClient, client.uploadClient, client.imageClient).forEach { assertIs<MockEngine>(it.engine) }
        assertNotSame(client.client, client.plainClient)
        assertNotSame(client.client, client.imageClient)
        assertEquals(2, discovery(client).search(query()).getOrThrow().items.size)
        val details = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
        assertEquals("Sandbox Cafe", details.getDetails("sandbox-cafe").getOrThrow().overview.title)
        val catalog = catalogs(client).catalogs().getOrThrow()
        assertEquals("Минск", catalog.cities.single().name)
        assertEquals(2, catalog.brewMethods.size)
        val drinks = createShopCheckInRepository(client.client, client.uploadClient).getDrinkOptions().getOrThrow()
        assertEquals("cappuccino", drinks.single().slug)
        val store = object : RoasterCatalogStore {
            private var saved: String? = null
            override suspend fun read() = saved
            override suspend fun write(value: String) { saved = value }
        }
        val roasters = createRoasterRepository(client.client, SandboxBackend.BASE_URL, store) { 0L }
        assertEquals(listOf("Sandbox Roaster", "Sandbox Light Lab", "Sandbox Decaf"), roasters.getRoasters().getOrThrow().map { it.name })
        assertEquals("sandbox-roaster", roasters.getRoaster("sandbox-roaster").getOrThrow().address.slug)
        assertContentEquals(byteArrayOf(1, 2, 3), client.client.get("${SandboxBackend.BASE_URL}/media/sample").body<ByteArray>())
        assertContentEquals(byteArrayOf(1, 2, 3), client.imageClient.get("${SandboxBackend.BASE_URL}/media/sample").body<ByteArray>())
        assertNull(backend.photo("https://example.org/media/sample"))
    } }

    @Test fun nearbyDiscoveryUsesRealSearchPayloadAndSharedFavoriteFlags() = runTest { withSandboxFixture(temporary) { _, client ->
        val repository = discovery(client)
        val origin = Coordinates(53.9, 27.56)
        val favorites = query(filters = DiscoveryFilters(favoritesOnly = true), origin = origin)
        assertEquals("Sandbox Cafe", repository.search(favorites).getOrThrow().items.single().title)
        sandboxAccountFavorites(client).setFavorite(sandboxShopFavorite("sandbox-cafe"), false).getOrThrow()
        assertTrue(repository.search(favorites).getOrThrow().items.isEmpty())
        assertEquals("Sandbox Lab", repository.search(query(filters = DiscoveryFilters(newOnly = true), origin = origin)).getOrThrow().items.single().title)
    } }

    @Test fun settingsFixturesServeTheCityCatalogThroughTheRealParsers() = runTest { withSandboxFixture(temporary) { backend, client ->
        assertEquals(listOf("Минск"), catalogs(client).catalogs().getOrThrow().cities.map { it.name })
        backend.cities = SandboxCities.Several
        assertEquals(listOf("Минск", "Брест", "Гродно"), catalogs(client).catalogs().getOrThrow().cities.map { it.name })
        backend.cities = SandboxCities.Empty
        assertTrue(catalogs(client).catalogs().getOrThrow().cities.isEmpty())
        backend.cities = SandboxCities.Failure
        assertTrue(catalogs(client).catalogs().isFailure)

        // reset() returns the fixture to its default; the scenarios still win over it.
        backend.reset(populated = true)
        assertEquals(listOf("Минск"), catalogs(client).catalogs().getOrThrow().cities.map { it.name })
        backend.scenario = SandboxScenario.ServerError
        assertTrue(catalogs(client).catalogs().isFailure)
    } }

    @Test fun discoveryPageFailureAllowsRetryAndRearmsOnRefresh() = runTest { withSandboxFixture(temporary) { backend, client ->
        backend.discovery = SandboxDiscovery.PageFailure
        val shops = discovery(client)
        assertEquals(20, shops.search(query(page = 1)).getOrThrow().items.size)
        assertTrue(shops.search(query(page = 2)).isFailure)
        assertEquals(2, shops.search(query(page = 2)).getOrThrow().currentPage)
        shops.search(query(page = 1)).getOrThrow()
        assertTrue(shops.search(query(page = 2)).isFailure)
    } }

    @Test fun discoveryRefreshFailurePreservesTheInitialLoadAndAllowsRetry() = runTest { withSandboxFixture(temporary) { backend, client ->
        backend.discovery = SandboxDiscovery.RefreshFailure
        val shops = discovery(client)
        val first = shops.search(query(page = 1)).getOrThrow()
        assertEquals(20, first.items.size)
        assertTrue(shops.search(query(page = 2)).isSuccess)
        assertTrue(shops.search(query(page = 1)).isFailure)
        assertEquals(first, shops.search(query(page = 1)).getOrThrow())
        val brest = query(filters = DiscoveryFilters(cityId = "brest"))
        assertTrue(shops.search(brest).isSuccess)
        assertTrue(shops.search(brest).isFailure)
        backend.discovery = SandboxDiscovery.Seeded
        backend.discovery = SandboxDiscovery.RefreshFailure
        assertTrue(shops.search(query(page = 1)).isSuccess)
        assertTrue(shops.search(query(page = 1)).isFailure)
    } }

    // The repository sends the map zoom plus one, so 21f is the server's detail level 22 and 9f is level 10.
    private val minsk = MapBounds(53.8, 27.4, 54.0, 27.7)
    private val brest = MapBounds(51.9, 23.5, 52.3, 24.0)

    @Test fun discoveryClustersUnfoldIntoTheSameShopsAtTheDetailZoom() = runTest { withSandboxFixture(temporary) { backend, client ->
        backend.discovery = SandboxDiscovery.Paged
        val shops = discovery(client)
        val criteria = DiscoveryCriteria(filters = DiscoveryFilters(cityId = "minsk"))
        val grouped = shops.mapContent(minsk, 9f, criteria).getOrThrow()
        val detailed = shops.mapContent(minsk, 21f, criteria).getOrThrow()
        assertTrue(grouped.clusters.isNotEmpty() || grouped.zones.isNotEmpty())
        assertEquals(detailed.markers.size, grouped.markers.size + grouped.clusters.sumOf { it.count } + grouped.zones.sumOf { it.shopCount })
        assertTrue(detailed.clusters.isEmpty())
        assertTrue(grouped.clusters.all {
            it.bounds.minLat <= it.coordinates.latitude && it.coordinates.latitude <= it.bounds.maxLat
        })
    } }

    @Test fun shopSearchUsesOnlyPostAndDoesNotRequireLocation() = runTest { withSandboxFixture(temporary) { backend, client ->
        assertEquals(HttpStatusCode.NotFound, client.client.get("/api/CoffeeShops").status)
        val shops = discovery(client)
        assertTrue(shops.search(query()).getOrThrow().items.all { it.distanceMeters == null })
        backend.discovery = SandboxDiscovery.Paged
        val ordinary = shops.search(query(filters = DiscoveryFilters(cityId = "brest"))).getOrThrow()
        assertEquals(9, ordinary.totalCount)
        assertTrue(ordinary.items.all { it.distanceMeters == null })
        assertTrue(shops.search(query(filters = DiscoveryFilters(newOnly = true, favoritesOnly = true))).getOrThrow().items.isEmpty())
        assertEquals(HttpStatusCode.NotFound, client.client.get("/api/CoffeeShops").status)
    } }

    @Test fun discoveryFixturesPageFilterSortAndFailThroughTheRealSearchAndMapServices() = runTest { withSandboxFixture(temporary) { backend, client ->
        val shops = discovery(client)
        suspend fun map(box: MapBounds, city: String? = null) =
            shops.mapContent(box, 21f, DiscoveryCriteria(filters = DiscoveryFilters(cityId = city))).getOrThrow()
        // The repository fixes the nearby radius at 3 km, so other radii go through the raw search request.
        val origin = Coordinates(53.9006, 27.5590)
        fun nearby(pageSize: Int = 100, page: Int = 1, favoritesOnly: Boolean = false) =
            query(filters = DiscoveryFilters(favoritesOnly = favoritesOnly), origin = origin, page = page, pageSize = pageSize)
        suspend fun rawNearbyTotal(radiusKm: Int): Int = client.client.post("/api/v1/coffee-shops/search") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("filters", buildJsonObject {
                    put("origin", buildJsonObject { put("latitude", origin.latitude); put("longitude", origin.longitude) })
                    put("radiusKm", radiusKm)
                })
                put("sort", "distance_asc"); put("page", 1); put("pageSize", 100)
            })
        }.body<JsonObject>()["data"]!!.jsonObject["totalItems"]!!.jsonPrimitive.int

        // The default fixture is the two core shops and the one-marker map.
        assertEquals(2, shops.search(query()).getOrThrow().items.size)

        backend.discovery = SandboxDiscovery.Paged
        val first = shops.search(query(page = 1, pageSize = 20)).getOrThrow()
        assertEquals(47, first.totalCount); assertEquals(3, first.totalPages); assertEquals(20, first.items.size)
        assertEquals(7, shops.search(query(page = 3, pageSize = 20)).getOrThrow().items.size)
        assertEquals(9, shops.search(query(filters = DiscoveryFilters(cityId = "brest"))).getOrThrow().totalCount)
        assertEquals(11, shops.search(query(text = "Discovery Shop 1")).getOrThrow().totalCount)
        assertEquals(15, shops.search(query(filters = DiscoveryFilters(roasterIds = setOf("sandbox-decaf")))).getOrThrow().totalCount)
        assertEquals(4, shops.search(query(filters = DiscoveryFilters(minRating = 4.9))).getOrThrow().totalCount)
        assertEquals(16, shops.search(query(filters = DiscoveryFilters(kind = ShopKind.Specialty))).getOrThrow().totalCount)
        assertEquals(0, shops.search(query(filters = DiscoveryFilters(cityId = "brest", roasterIds = setOf("sandbox-decaf"), tagIds = setOf("none")))).getOrThrow().totalCount)

        // The nearby search is sorted by distance, limited to the radius, paged, and follows the shared favorites.
        val near = shops.search(nearby()).getOrThrow()
        val distances = near.items.mapNotNull { it.distanceMeters }
        assertEquals(near.items.size, distances.size); assertEquals(distances.sorted(), distances); assertTrue(distances.all { it <= 3_000.0 })
        assertEquals(near.totalCount, rawNearbyTotal(radiusKm = 3))
        assertTrue(rawNearbyTotal(radiusKm = 1) < near.totalCount)
        val tenPerPage = shops.search(nearby(pageSize = 10)).getOrThrow()
        assertEquals((near.totalCount + 9) / 10, tenPerPage.totalPages); assertEquals(minOf(10, near.totalCount), tenPerPage.items.size)
        assertEquals("Sandbox Cafe", shops.search(nearby(favoritesOnly = true)).getOrThrow().items.single().title)
        sandboxAccountFavorites(client).setFavorite(sandboxShopFavorite("discovery-3"), true).getOrThrow()
        assertEquals(2, shops.search(nearby(favoritesOnly = true)).getOrThrow().items.size)

        // The map: shops by bounds and filters, zones inside the bounds, no clusters, nothing truncated.
        val inMinsk = map(minsk)
        assertEquals(38, inMinsk.markers.size); assertTrue(inMinsk.zones.isEmpty()); assertTrue(inMinsk.clusters.isEmpty()); assertFalse(inMinsk.isTruncated)
        assertEquals(9, map(brest).markers.size); assertTrue(map(brest).zones.isEmpty())
        assertTrue(map(minsk, city = "brest").markers.isEmpty())

        backend.discovery = SandboxDiscovery.Truncated
        assertEquals(SandboxDiscoveryBackend.MAP_LIMIT, map(minsk).markers.size); assertTrue(map(minsk).isTruncated)
        assertFalse(map(brest).isTruncated)

        backend.discovery = SandboxDiscovery.Empty
        assertEquals(0, shops.search(query()).getOrThrow().totalCount)
        assertTrue(shops.search(nearby()).getOrThrow().items.isEmpty())
        assertTrue(map(minsk).markers.isEmpty()); assertTrue(map(minsk).zones.isEmpty())

        backend.discovery = SandboxDiscovery.SearchFailure
        assertTrue(shops.search(query()).isFailure); assertTrue(shops.search(nearby()).isFailure)
        assertTrue(shops.mapContent(minsk, 21f, DiscoveryCriteria()).isSuccess)

        backend.discovery = SandboxDiscovery.MapFailure
        assertTrue(shops.mapContent(minsk, 21f, DiscoveryCriteria()).isFailure)
        assertEquals(47, shops.search(query()).getOrThrow().totalCount)

        backend.discovery = SandboxDiscovery.Paged
        backend.reset(true)
        assertEquals(SandboxDiscovery.Seeded, backend.discovery)
        assertEquals(2, shops.search(query()).getOrThrow().items.size)
    } }
}
