package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the behaviour of the legacy map view model before it moves: the debounced bounds load and the prefetch area, the kept zones, the failed load, the selection and
 * search races, the nearby carousel, the camera commands and the zone card. They run on the real view model with in-memory repositories.
 */
class MapViewModelParityTest {
    private val minsk = MapBounds(53.88, 27.52, 53.92, 27.60)

    @Test
    fun aBoundsLoadIsDebouncedUsesThePrefetchAreaAndTheCityAndSkipsPansInsideIt() = parity {
        vm.onBoundsChanged(minsk, 14f)
        val first = mapCalls.next(14f)
        assertEquals(minsk.expandedForPrefetch(), first.bounds); assertEquals("minsk", first.filters.cityId)
        vm.state.first { !it.isLoading && it.activeBounds == minsk }

        vm.onBoundsChanged(MapBounds(53.885, 27.525, 53.915, 27.595), 14f)
        delay(450)
        assertNull(mapCalls.tryReceive().getOrNull()?.takeIf { it.zoom == 14f })

        vm.onBoundsChanged(minsk, 15f)
        assertEquals(15f, mapCalls.next(15f).zoom)
    }

    @Test
    fun zonesSeenInTheCityAreKeptAndForgottenOnACityChange() = parity(
        content = { call ->
            when {
                call.zoom == 22f -> MapContent()
                call.filters.cityId == "brest" -> MapContent(zones = listOf(zone("z3")))
                call.zoom == 14f -> MapContent(zones = listOf(zone("z1")))
                else -> MapContent(zones = listOf(zone("z2")))
            }
        },
    ) {
        vm.onBoundsChanged(minsk, 14f); mapCalls.next(14f)
        vm.state.first { it.zones.map(MapCoffeeZone::id) == listOf("z1") }
        vm.onBoundsChanged(minsk, 15f); mapCalls.next(15f)
        assertEquals(listOf("z2", "z1"), vm.state.first { it.zones.size == 2 }.zones.map { it.id })

        preference.select("brest")
        assertEquals("brest", mapCalls.nextForCity("brest").filters.cityId)
        assertEquals(listOf("z3"), vm.state.first { it.zones.map(MapCoffeeZone::id) == listOf("z3") }.zones.map { it.id })
    }

    @Test
    fun aFailedLoadOffersToSearchTheAreaAndARetrySucceeds() {
        var fail = true
        parity(content = { call -> if (call.zoom == 22f) MapContent() else if (fail) null else MapContent(shops = listOf(mapShop("s1")), isTruncated = true) }) {
            vm.onBoundsChanged(minsk, 14f); mapCalls.next(14f)
            val failed = vm.state.first { it.showSearchArea }
            assertFalse(failed.isLoading)
            fail = false
            vm.searchCurrentArea(); mapCalls.next(14f)
            val loaded = vm.state.first { it.shops.any { shop -> shop.id == "s1" } && !it.showSearchArea }
            assertTrue(loaded.isTruncated)
        }
    }

    @Test
    fun theNearbyCarouselKeepsTheTenNearestInsideTheRadiusAndMergesThemIntoTheMap() = parity(
        content = { call ->
            if (call.zoom != 22f) MapContent()
            else MapContent(shops = listOf(mapShop("far", 54.5, 27.56), mapShop("b", 53.9, 27.561), mapShop("a", 53.9, 27.561), mapShop("near", 53.9, 27.5601)) + (1..12).map { mapShop("n$it", 53.9 + it * 0.0005, 27.56) })
        },
    ) {
        vm.onNearbyOriginChanged(91.0, 27.56); vm.onNearbyOriginChanged(53.9, 181.0)
        assertNull(vm.state.value.nearbyOrigin)
        vm.onNearbyOriginChanged(53.9, 27.56)
        val call = mapCalls.next(22f)
        assertEquals(nearbyMapBounds(com.coffeepeek.admin.location.GeoPoint(53.9, 27.56)), call.bounds); assertEquals("minsk", call.filters.cityId)
        val state = vm.state.first { it.nearbyShops.isNotEmpty() }
        assertEquals(10, state.nearbyShops.size); assertEquals("near", state.nearbyShops.first().id)
        assertFalse(state.nearbyShops.any { it.id == "far" })
        assertTrue(state.shops.map { it.id }.containsAll(state.nearbyShops.map { it.id }))
        assertEquals(listOf("a", "b"), state.nearbyShops.map { it.id }.filter { it == "a" || it == "b" })
    }

    @Test
    fun selectingAnotherShopDropsALateDetailsAnswer() {
        val details = mapOf("a" to CompletableDeferred<Result<CoffeeShopDetails>>(), "b" to CompletableDeferred<Result<CoffeeShopDetails>>())
        parity(details = { id -> details.getValue(id).await() }) {
            vm.onShopSelected(mapShop("a")); vm.onShopSelected(mapShop("b"))
            assertTrue(vm.state.value.isLoadingShopDetails)
            details.getValue("b").complete(Result.success(detail("b")))
            vm.state.first { it.selectedShopDetails?.shop?.id == "b" }
            details.getValue("a").complete(Result.success(detail("a")))
            delay(150)
            assertEquals("b", vm.state.value.selectedShopDetails?.shop?.id); assertEquals("b", vm.state.value.selectedShop?.id)
            assertFalse(vm.state.value.isLoadingShopDetails)
        }
    }

    @Test
    fun theDetailsOfAShopAreLoadedOnceAndSelectingItAgainUsesTheCache() = parity {
        vm.onShopSelected(mapShop("a")); vm.state.first { it.selectedShopDetails?.shop?.id == "a" }
        vm.onShopSelected(mapShop("b")); vm.state.first { it.selectedShopDetails?.shop?.id == "b" }
        vm.onShopSelected(mapShop("a"))
        assertEquals("a", vm.state.value.selectedShopDetails?.shop?.id)
        assertEquals(listOf("a", "b"), detailCalls)
    }

    @Test
    fun searchIsDebouncedTrimmedAndAStaleAnswerIsIgnored() {
        val stale = CompletableDeferred<Unit>()
        parity(search = { filters ->
            if (filters.query == "a") stale.await()
            Result.success(PagedResult(listOf(searchShop(filters.query.orEmpty())), 1, 1, 1))
        }) {
            vm.onQueryChange("a")
            assertTrue(vm.state.value.isSearchLoading); assertTrue(vm.state.value.showSearchSuggestions)
            val first = searchCalls.receive(); assertEquals("a", first.query); assertEquals(3, first.pageSize); assertEquals("minsk", first.cityId)
            vm.onQueryChange(" ab ")
            assertEquals("ab", searchCalls.receive().query)
            vm.state.first { it.searchResults.map(CoffeeShop::id) == listOf("ab") && !it.isSearchLoading }
            stale.complete(Unit)
            delay(150)
            assertEquals(listOf("ab"), vm.state.value.searchResults.map { it.id })
        }
    }

    @Test
    fun searchResultsWithoutCoordinatesAreDroppedAndAFailureIsShown() {
        var fail = false
        parity(search = {
            if (fail) Result.failure(IllegalStateException("offline"))
            else Result.success(PagedResult(listOf(searchShop("with"), searchShop("without", located = false)), 2, 1, 1))
        }) {
            vm.onQueryChange("w"); searchCalls.receive()
            assertEquals(listOf("with"), vm.state.first { !it.isSearchLoading }.searchResults.map { it.id })
            fail = true
            vm.onQueryChange("wx"); searchCalls.receive()
            val failed = vm.state.first { it.searchFailed }
            assertTrue(failed.searchResults.isEmpty()); assertFalse(failed.isSearchLoading)
            vm.onQueryChange("")
            assertFalse(vm.state.value.showSearchSuggestions); assertFalse(vm.state.value.searchFailed); assertTrue(vm.state.value.searchResults.isEmpty())
        }
    }

    @Test
    fun selectingASearchResultClearsTheSearchMovesTheCameraAndSelectsTheShop() = parity {
        val shop = searchShop("found")
        vm.onSearchResultSelected(shop)
        val state = vm.state.first { it.selectedShop?.id == "found" }
        assertEquals("", state.query); assertFalse(state.showSearchSuggestions); assertTrue(state.searchResults.isEmpty())
        assertEquals(53.9 to 27.56, state.cameraTarget); assertEquals(16f, state.cameraZoom)
        assertTrue(state.shops.any { it.id == "found" })
        vm.onCameraTargetApplied()
        assertNull(vm.state.value.cameraTarget); assertNull(vm.state.value.cameraZoom)
    }

    @Test
    fun aCarouselCardSelectsTheShopAndMovesTheCameraToZoom16() = parity {
        vm.onCarouselShopSelected(mapShop("c", 53.91, 27.57))
        val state = vm.state.value
        assertEquals("c", state.selectedShop?.id); assertEquals(53.91 to 27.57, state.cameraTarget); assertEquals(16f, state.cameraZoom)
    }

    @Test
    fun aShopFocusFromTheShopScreenSelectsItAndFocusesTheCamera() = parity {
        vm.focusOnShop(Navigator.MapShopFocus("s1", 53.9, 27.56, "Cafe"))
        val state = vm.state.first { it.selectedShop?.id == "s1" && it.cameraZoom != null }
        assertEquals(53.9 to 27.56, state.cameraTarget); assertEquals(16f, state.cameraZoom); assertEquals("Cafe", state.selectedShop?.title)
    }

    @Test
    fun zoomMovesTheCameraToTheViewportCenterAndStaysInsideTheLimits() = parity {
        vm.onBoundsChanged(minsk, 14f)
        vm.zoomOut()
        val out = vm.state.value
        assertEquals(13f, out.cameraZoom); assertEquals((minsk.minLat + minsk.maxLat) / 2 to (minsk.minLon + minsk.maxLon) / 2, out.cameraTarget)
        vm.onCameraTargetApplied()
        vm.onBoundsChanged(minsk, 20f); vm.zoomIn()
        assertNull(vm.state.value.cameraZoom)
        vm.onBoundsChanged(minsk, 3f); vm.zoomOut()
        assertNull(vm.state.value.cameraZoom)
    }

    @Test
    fun aZoneCardZoomsToTheZoneAndTheZonesCanBeHidden() = parity {
        val zone = zone("z", lat = 53.93, lon = 27.57)
        vm.onZoneSelected(zone)
        assertEquals(zone, vm.state.value.selectedZone)
        vm.showSelectedZoneShops()
        val state = vm.state.value
        assertNull(state.selectedZone); assertEquals(53.93 to 27.57, state.cameraTarget); assertEquals(14.5f, state.cameraZoom)
        vm.onZoneSelected(zone); vm.toggleZones()
        assertFalse(vm.state.value.showZones); assertNull(vm.state.value.selectedZone)
        vm.toggleZones(); assertTrue(vm.state.value.showZones)
    }

    private fun parity(
        content: suspend (MapCall) -> MapContent? = { MapContent() },
        details: suspend (String) -> Result<CoffeeShopDetails> = { Result.success(detail(it)) },
        search: suspend (ShopFilters) -> Result<PagedResult<CoffeeShop>> = { Result.success(PagedResult(emptyList(), 0, 0, 1)) },
        test: suspend MapFixture.() -> Unit,
    ) = runBlocking {
        val fixture = MapFixture(content, details, search)
        try { withTimeout(10_000) { fixture.test() } } finally { fixture.vm.close() }
    }
}

private class MapCall(val bounds: MapBounds, val zoom: Float, val filters: ShopFilters)

private suspend fun Channel<MapCall>.next(zoom: Float): MapCall { while (true) { val call = receive(); if (call.zoom == zoom) return call } }
private suspend fun Channel<MapCall>.nextForCity(city: String): MapCall { while (true) { val call = receive(); if (call.filters.cityId == city && call.zoom != 22f) return call } }

private fun mapShop(id: String, lat: Double = 53.9, lon: Double = 27.56) = MapShop(id = id, title = "Shop $id", latitude = lat, longitude = lon)

private fun zone(id: String, lat: Double = 53.9, lon: Double = 27.56) =
    MapCoffeeZone(id = id, name = "Zone $id", description = "", latitude = lat, longitude = lon, radiusMeters = 500.0, shopCount = 3)

private fun searchShop(id: String, located: Boolean = true) = CoffeeShop(
    id = id, title = "Shop $id", rating = null, cityName = null, priceRange = null, photoUrl = null,
    location = if (located) ShopLocation(address = "Street", latitude = 53.9, longitude = 27.56) else ShopLocation(address = "Street"),
)

private fun detail(id: String) = CoffeeShopDetails(shop = CoffeeShop(id = id, title = "Shop $id", rating = null, cityName = null, priceRange = null, photoUrl = null))

private class MapFixture(
    content: suspend (MapCall) -> MapContent?, details: suspend (String) -> Result<CoffeeShopDetails>, search: suspend (ShopFilters) -> Result<PagedResult<CoffeeShop>>,
) {
    val mapCalls = Channel<MapCall>(Channel.UNLIMITED)
    val searchCalls = Channel<ShopFilters>(Channel.UNLIMITED)
    val detailCalls = mutableListOf<String>()
    val preference = CityPreference(MapSettings)
    val vm = MapViewModel(MapShops(mapCalls, searchCalls, detailCalls, content, details, search), preference)
}

private class MapShops(
    private val mapCalls: Channel<MapCall>, private val searchCalls: Channel<ShopFilters>, private val detailCalls: MutableList<String>,
    private val content: suspend (MapCall) -> MapContent?, private val details: suspend (String) -> Result<CoffeeShopDetails>,
    private val search: suspend (ShopFilters) -> Result<PagedResult<CoffeeShop>>,
) : ShopRepository {
    override suspend fun getCatalogs() = Result.success(ShopCatalogs(cities = listOf(City("minsk", "Минск"), City("brest", "Брест"))))
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> {
        val call = MapCall(bounds, zoom, filters)
        mapCalls.send(call)
        return content(call)?.let { Result.success(it) } ?: Result.failure(IllegalStateException("offline"))
    }
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> { searchCalls.send(filters); return search(filters) }
    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> { detailCalls += id; return details(id) }
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("Unused")
    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("Unused")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

private object MapSettings : SettingRepository {
    override suspend fun save(model: Setting) = Unit
    override suspend fun read(key: String): Setting? = null
    override fun readFlow(key: String) = flowOf<Setting?>(null)
    override suspend fun readAll() = emptyList<Setting>()
    override fun readAllFlow() = flowOf(emptyList<Setting>())
    override suspend fun delete(key: String) = Unit
}
