package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
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

    @Test fun listTypingKeepsMapCriteriaWithoutMapSuggestionsOrSearchEvents() = parity(presentationActive = false) {
        withFeed { feed ->
            withMapAnalytics { transport ->
                feed.setSearchAnalyticsActive(true)
                feed.onQueryChange("laptop")
                vm.syncSearch("laptop", FeedFiltersUi(cityId = "minsk", tagIds = setOf("laptop-friendly")), null)
                assertEquals(20, searchCalls.receive().pageSize)
                feed.uiState.first { it.query == "laptop" && !it.isLoading && !it.isRefreshing }
                delay(500)
                assertTrue(searchCalls.tryReceive().isFailure)
                assertEquals(listOf("list"), transport.searchSources("catalog_search_started"))
                assertEquals("laptop", vm.state.value.query)
                assertEquals(setOf("laptop-friendly"), vm.state.value.filters.tagIds)
                assertFalse(vm.state.value.showSearchSuggestions)
                vm.setSearchActive(true)
                delay(500)
                assertTrue(searchCalls.tryReceive().isFailure)
            }
        }
    }

    @Test fun mapTypingRecordsOneMapSearchAndKeepsTheHiddenListUpdatedWithoutListSearch() = parity {
        withFeed { feed ->
            withMapAnalytics { transport ->
                feed.setSearchAnalyticsActive(false)
                vm.onQueryChange("espresso")
                feed.onQueryChange("espresso")
                vm.syncSearch("espresso", feed.uiState.value.filters, feed.uiState.value.searchOrigin)
                val calls = listOf(searchCalls.receive(), searchCalls.receive())
                assertEquals(setOf(3, 20), calls.map { it.pageSize }.toSet())
                vm.state.first { !it.isSearchLoading }
                feed.uiState.first { !it.isLoading && !it.isRefreshing }
                delay(50)
                assertEquals(listOf("map"), transport.searchSources("catalog_search_started"))
                assertEquals(listOf("map"), transport.searchSources("catalog_results_viewed"))
                assertEquals("espresso", feed.uiState.value.query)
            }
        }
    }

    @Test fun leavingTheMapCancelsDebouncedSuggestions() = parity {
        withMapAnalytics { transport ->
            vm.onQueryChange("espresso")
            vm.setSearchActive(false)
            delay(500)
            assertTrue(searchCalls.tryReceive().isFailure)
            assertTrue(transport.searchSources("catalog_search_started").isEmpty())
            assertFalse(vm.state.value.showSearchSuggestions)
        }
    }

    @Test fun leavingTheMapRejectsLateSearchSuccessAndFailureTelemetry() {
        assertHiddenSearchCompletion(Result.success(PagedResult(listOf(searchShop("late")), 1, 1, 1)))
        assertHiddenSearchCompletion(Result.failure(IllegalStateException("offline")))
    }

    private fun assertHiddenSearchCompletion(result: Result<PagedResult<CoffeeShop>>) {
        val reply = CompletableDeferred<Result<PagedResult<CoffeeShop>>>()
        parity(search = { withContext(NonCancellable) { reply.await() } }) {
            withMapAnalytics { transport ->
                vm.onQueryChange("espresso")
                searchCalls.receive()
                vm.setSearchActive(false)
                vm.setSearchActive(true)
                reply.complete(result)
                delay(150)
                assertEquals(listOf("map"), transport.searchSources("catalog_search_started"))
                assertTrue(transport.searchSources("catalog_results_viewed").isEmpty())
                assertTrue(transport.searchSources("catalog_search_failed").isEmpty())
                assertTrue(vm.state.value.searchResults.isEmpty())
                assertFalse(vm.state.value.isSearchLoading)
            }
        }
    }

    @Test fun hidingAndRestoringTheListRejectsItsOldSearchCompletion() {
        val reply = CompletableDeferred<Result<PagedResult<CoffeeShop>>>()
        parity(search = { if (it.query == "late") reply.await() else Result.success(PagedResult(emptyList(), 0, 0, 1)) }) {
            withFeed { feed ->
                withMapAnalytics { transport ->
                    feed.setSearchAnalyticsActive(true)
                    feed.onQueryChange("late")
                    searchCalls.receive()
                    feed.setSearchAnalyticsActive(false)
                    feed.setSearchAnalyticsActive(true)
                    reply.complete(Result.success(PagedResult(listOf(searchShop("late")), 1, 1, 1)))
                    feed.uiState.first { it.shops.isNotEmpty() }
                    delay(50)
                    assertEquals(listOf("list"), transport.searchSources("catalog_search_started"))
                    assertTrue(transport.searchSources("catalog_results_viewed").isEmpty())
                }
            }
        }
    }

    @Test fun changedFavoriteMembershipRefreshesTheMapAndDropsTheSelectedCard() {
        val membership = MutableStateFlow(Result.success(setOf("favorite")))
        parity(membership = membership, content = { MapContent(shops = membership.value.getOrThrow().map { mapShop(it) }) }) {
            vm.syncSearch("", com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi(cityId = "minsk", favoritesOnly = true), null)
            vm.onBoundsChanged(minsk, 14f)
            mapCalls.next(14f)
            vm.state.first { it.shops.isNotEmpty() && it.nearbyShops.isNotEmpty() }
            vm.onShopSelected(mapShop("favorite"))
            vm.state.first { it.selectedShopDetails != null }
            membership.value = Result.success(emptySet())
            mapCalls.next(14f)
            val state = vm.state.first { !it.isLoading && it.shops.isEmpty() && it.nearbyShops.isEmpty() }
            assertNull(state.selectedShop)
            assertNull(state.selectedShopDetails)
            assertTrue(state.filters.favoritesOnly)
        }
    }

    @Test fun replacingAnActiveAccountClearsTheCardCacheAndRejectsLateViewportReplies() {
        val sessions = MapSessions()
        val late = DeferredMapReply()
        parity(sessions = sessions, content = { call ->
            if (call.filters.query == "late" && sessions.peekSession()?.userId == "a") late.await(call.zoom)
            else MapContent()
        }) {
            cacheCard("cached-a")
            vm.syncSearch("late", com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi(cityId = "minsk", visitedOnly = true), null)
            vm.onBoundsChanged(minsk, 14f)
            mapCalls.next(14f)
            late.awaitStarted(14f)
            sessions.changeAccount("b")
            sessions.awaitObserved("b")
            assertTrue(vm.state.value.cardDetails.isEmpty())
            late.release(MapContent(shops = listOf(mapShop("private-a"))))
            delay(150)
            assertTrue(vm.state.value.shops.none { it.id == "private-a" })
            assertTrue(vm.state.value.nearbyShops.none { it.id == "private-a" })
        }
    }

    @Test fun replacingAnActiveAccountRejectsLateCardDetails() {
        val sessions = MapSessions()
        val late = CompletableDeferred<Result<CoffeeShopDetails>>()
        val started = CompletableDeferred<Unit>()
        parity(sessions = sessions, details = {
            started.complete(Unit)
            withContext(NonCancellable) { late.await() }
        }) {
            vm.onShopSelected(mapShop("private-a"))
            started.await()
            sessions.changeAccount("b")
            sessions.awaitObserved("b")
            late.complete(Result.success(detail("private-a")))
            delay(150)
            assertTrue(vm.state.value.cardDetails.isEmpty())
            assertNull(vm.state.value.selectedShop)
            assertNull(vm.state.value.selectedShopDetails)
        }
    }

    @Test fun sharedSearchCriteriaReachViewportAndNearbyCards() = parity {
        val filters = com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi(cityId = "minsk",
            tagIds = setOf("laptop-friendly"), favoritesOnly = true, openOnly = true)
        vm.syncSearch("Coffee", filters, null)
        vm.onBoundsChanged(minsk, 14f)
        val nearby = mapCalls.next(22f)
        val viewport = mapCalls.next(14f)
        assertEquals("Coffee", viewport.filters.query)
        assertEquals(listOf("laptop-friendly"), viewport.filters.tagIds)
        assertTrue(viewport.filters.favoritesOnly && viewport.filters.openOnly)
        assertEquals(viewport.filters, nearby.filters)
    }

    @Test fun cachedCardDetailsRemainAvailableWhenAnotherCardIsSelectedAndOnReturn() = parity {
        vm.onShopSelected(mapShop("a"))
        vm.state.first { it.cardDetails.containsKey("a") }
        vm.onShopSelected(mapShop("b"))
        val state = vm.state.first { it.cardDetails.containsKey("b") }
        assertEquals(setOf("a", "b"), state.cardDetails.keys)
        vm.onShopSelected(mapShop("a"))
        assertEquals("a", vm.state.value.selectedShopDetails?.shop?.id)
        assertFalse(vm.state.value.isLoadingShopDetails)
        assertEquals(listOf("a", "b"), detailCalls)
    }

    @Test fun choosingASuggestionPreservesTheSharedListQueryAndFilters() = parity {
        val filters = com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi(cityId = "minsk", tagIds = setOf("laptop-friendly"))
        vm.syncSearch("Coffee", filters, null)
        vm.onSearchResultSelected(searchShop("found"))
        val selected = vm.state.first { it.selectedShop?.id == "found" }
        assertEquals("Coffee", selected.query)
        assertEquals(filters, selected.filters)
        assertFalse(selected.showSearchSuggestions)
    }

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
    fun aFilterChangeAutomaticallyReloadsAfterAFailedRequest() {
        var fail = true
        parity(content = { call -> if (call.zoom == 22f) MapContent() else if (fail) null else MapContent(shops = listOf(mapShop("s1")), isTruncated = true) }) {
            vm.onBoundsChanged(minsk, 14f); mapCalls.next(14f)
            val failed = vm.state.first { !it.isLoading }
            assertFalse(failed.isLoading)
            fail = false
            vm.syncSearch("", com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi(cityId = "minsk", openOnly = true), null)
            assertTrue(mapCalls.next(14f).filters.openOnly)
            val loaded = vm.state.first { it.shops.any { shop -> shop.id == "s1" } }
            assertTrue(loaded.isTruncated)
        }
    }

    @Test
    fun theNearbyCarouselKeepsTheTenNearestWithoutDuplicatingClusteredPoints() = parity(
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
        assertTrue(state.shops.isEmpty())
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
    fun searchResultsWithoutCoordinatesAreRetainedAndAFailureIsShown() {
        var fail = false
        parity(search = {
            if (fail) Result.failure(IllegalStateException("offline"))
            else Result.success(PagedResult(listOf(searchShop("with"), searchShop("without", located = false)), 2, 1, 1))
        }) {
            vm.onQueryChange("w"); searchCalls.receive()
            assertEquals(listOf("with", "without"), vm.state.first { !it.isSearchLoading }.searchResults.map { it.id })
            fail = true
            vm.onQueryChange("wx"); searchCalls.receive()
            val failed = vm.state.first { it.searchFailed }
            assertTrue(failed.searchResults.isEmpty()); assertFalse(failed.isSearchLoading)
            vm.onQueryChange("")
            assertFalse(vm.state.value.showSearchSuggestions); assertFalse(vm.state.value.searchFailed); assertTrue(vm.state.value.searchResults.isEmpty())
        }
    }

    @Test
    fun selectingAnUnlocatedResultResolvesCoordinatesFromDetails() = parity(details = { Result.success(CoffeeShopDetails(shop = searchShop(it))) }) {
        vm.onSearchResultSelected(searchShop("found", located = false))
        val state = vm.state.first { it.selectedShop?.id == "found" }
        assertEquals(53.9 to 27.56, state.cameraTarget)
        assertEquals(16f, state.cameraZoom)
        assertFalse(state.isSearchLoading)
        assertEquals(listOf("found"), detailCalls)
    }

    @Test
    fun failedUnlocatedSelectionRetainsTheQueryAndShowsFailure() = parity(details = { Result.failure(IllegalStateException("offline")) }) {
        vm.onSearchResultSelected(searchShop("found", located = false))
        val state = vm.state.first { it.searchFailed }
        assertFalse(state.isSearchLoading)
        assertNull(state.selectedShop)
        assertNull(state.cameraTarget)
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
        sessions: MapSessions = MapSessions(),
        membership: MutableStateFlow<Result<Set<String>>> = MutableStateFlow(Result.success(emptySet())),
        presentationActive: Boolean = true,
        test: suspend MapFixture.() -> Unit,
    ) = runBlocking {
        val fixture = MapFixture(content, details, search, sessions, membership)
        try {
            withTimeout(10_000) {
                sessions.awaitObserved("a")
                fixture.membershipObserved.await()
                fixture.vm.setSearchActive(presentationActive)
                fixture.test()
            }
        } finally { fixture.vm.close() }
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
    private val sessions: MapSessions, membership: MutableStateFlow<Result<Set<String>>>,
) {
    val mapCalls = Channel<MapCall>(Channel.UNLIMITED)
    val searchCalls = Channel<ShopFilters>(Channel.UNLIMITED)
    val detailCalls = mutableListOf<String>()
    val membershipObserved = CompletableDeferred<Unit>()
    val preference = CityPreference(MapSettings)
    private val shops = MapShops(mapCalls, searchCalls, detailCalls, content, details, search)
    val vm = MapViewModel(shops, preference, MapContentSource(shops::getMapContent), sessions, flow {
        membership.collect { result ->
            emit(result)
            membershipObserved.complete(Unit)
        }
    })

    suspend fun cacheCard(id: String) {
        vm.onShopSelected(mapShop(id))
        vm.state.first { id in it.cardDetails }
    }

    suspend fun withFeed(test: suspend (FeedViewModel) -> Unit) {
        val feed = FeedViewModel(shops, UnusedMapFavorites, preference, sessions)
        try {
            searchCalls.receive()
            feed.uiState.first { !it.isLoading && !it.isRefreshing }
            delay(500)
            test(feed)
        } finally { feed.close() }
    }
}

private suspend fun withMapAnalytics(test: suspend (RecordingTransport) -> Unit) {
    val previous = Analytics.service
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    val transport = RecordingTransport()
    val service = AnalyticsService(AnalyticsContext("coffee-peek-ios", "1.0+1", "development"),
        MemoryConsent(AnalyticsConsent.Accepted), transport, scope, enabled = true)
    service.setForeground(true)
    service.restoreConsent()
    service.syncIdentity("a")
    Analytics.install(service)
    try { test(transport) } finally {
        service.setForeground(false)
        previous?.let(Analytics::install)
        scope.cancel()
    }
}

private fun RecordingTransport.searchSources(event: String) = events.filter { it.first == event }.map { it.second["source"] }

private object UnusedMapFavorites : FavoriteRepository {
    override suspend fun getFavoriteIds() = emptySet<String>()
    override suspend fun isFavorite(shopId: String) = false
    override suspend fun getFavorites() = Result.success(emptyList<CoffeeShopDetails>())
    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Unused")
    override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Unused")
    override suspend fun clearAll() = Unit
}

private class DeferredMapReply {
    private val value = CompletableDeferred<MapContent>()
    private val started = Channel<Float>(Channel.UNLIMITED)
    private val returned = Channel<Unit>(Channel.UNLIMITED)

    suspend fun await(zoom: Float): MapContent = withContext(NonCancellable) {
        started.send(zoom)
        value.await().also { returned.send(Unit) }
    }

    suspend fun awaitStarted(zoom: Float) {
        while (started.receive() != zoom) Unit
    }

    suspend fun release(content: MapContent) {
        value.complete(content)
        returned.receive()
    }
}

private class MapSessions : SessionRepository {
    private val current = MutableStateFlow<Session?>(Session(accessToken = "a-token", userId = "a"))
    private val observed = Channel<String?>(Channel.UNLIMITED)
    override fun peekSession() = current.value
    override fun applySession(session: Session?) { current.value = session }
    override fun isActiveSession(session: Session?) = session != null && session == current.value
    override suspend fun getSession() = current.value
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override suspend fun isLoggedIn() = current.value != null
    override fun observeSession() = flow {
        current.collect { session ->
            emit(session)
            observed.send(session?.userId)
        }
    }

    fun changeAccount(userId: String) { applySession(Session(accessToken = "$userId-token", userId = userId)) }
    suspend fun awaitObserved(userId: String) {
        while (observed.receive() != userId) Unit
    }
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
