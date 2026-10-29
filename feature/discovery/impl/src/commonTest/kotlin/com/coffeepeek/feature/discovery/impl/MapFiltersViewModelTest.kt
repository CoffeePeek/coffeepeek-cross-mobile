package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.domain.model.*


import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapFiltersViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private val city = FakeCity()
    private val location = FakeLocation()
    private val analytics = RecordingAnalytics()
    private val account = FakeAccount()
    private val favorites = FakeFavorites()
    private val view = MapBounds(53.88, 27.50, 53.92, 27.60)
    private val laptop = DiscoveryFilters(cityId = "minsk", tagIds = setOf("laptop-friendly"))

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model() = MapViewModel(repository, city, location, FakeShopDetails(), analytics, account, favorites).also {
        store.put("map", it)
        runCurrent()
        it.onAction(MapAction.BoundsChanged(view, 14f))
        runCurrent()
        repository.mapRequests.clear()
        repository.mapCriteria.clear()
    }

    private fun TestScope.settle() { advanceTimeBy(501); runCurrent() }

    @Test fun sameFiltersReachMarkersCarouselAndMapSuggestions() = runTest(dispatcher) {
        val vm = model()
        val filters = laptop.copy(openOnly = true, newOnly = true, visitedOnly = true, favoritesOnly = true)
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria("coffee", filters)))
        runCurrent()
        settle()
        assertEquals(setOf(14f, 22f), repository.mapRequests.map { it.second }.toSet())
        assertTrue(repository.mapRequests.all { it.third == filters })
        assertTrue(repository.mapCriteria.all { it.text == "coffee" })
        assertEquals(filters, repository.queries.single().filters)
        assertTrue(analytics.mapSearches.single().contains("tags"))
        assertTrue(analytics.mapSearches.single().contains("favoritesOnly"))
    }

    @Test fun changingFiltersClearsOldSelectionZonesAndViewportCache() = runTest(dispatcher) {
        val zone = MapZone("zone", "Zone", "", null, Coordinates(53.9, 27.56), 300.0, 2)
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("old")), zones = listOf(zone))) }
        val vm = model()
        settle()
        vm.onAction(MapAction.SelectMarker("old"))
        runCurrent()
        val filtered = CompletableDeferred<Result<MapContent>>()
        repository.map = { _, _, _ -> filtered.await() }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = laptop)))
        runCurrent()
        assertNull(vm.state.value.selectedShop)
        assertTrue(vm.state.value.zones.isEmpty())
        assertTrue(vm.state.value.nearby.isEmpty())
        filtered.complete(Result.success(MapContent(listOf(marker("filtered")))))
        settle()
        assertEquals(listOf("filtered"), vm.state.value.markers.map { it.id })
        assertEquals(listOf("filtered"), vm.state.value.nearby.map { it.id })
    }

    @Test fun lateCancelledViewportRepliesCannotRestoreOldFiltersResults() = runTest(dispatcher) {
        val late = CompletableDeferred<Result<MapContent>>()
        repository.map = { _, _, _ -> withContext(NonCancellable) { late.await() } }
        val vm = model()
        settle()
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("filtered")))) }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = laptop)))
        runCurrent()
        settle()
        late.complete(Result.success(MapContent(listOf(marker("old")))))
        runCurrent()
        assertEquals(listOf("filtered"), vm.state.value.markers.map { it.id })
        assertEquals(listOf("filtered"), vm.state.value.nearby.map { it.id })
    }

    @Test fun changingQuickSwitchesReloadsTheMap() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = laptop)))
        runCurrent()
        settle()
        val calls = repository.mapRequests.size
        val filters = laptop.copy(openOnly = true, newOnly = true)
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = filters)))
        runCurrent()
        settle()
        assertEquals(calls + 2, repository.mapRequests.size)
        assertEquals(filters, vm.state.value.filters)
    }

    @Test fun searchSelectionsReachMapAndMapEditsRemainInTheList() = runTest(dispatcher) {
        val map = model()
        val search = SearchViewModel(repository, FakeFavorites(), city, location, FakeAccount(), analytics)
        store.put("search", search)
        backgroundScope.launch { search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) } }
        runCurrent()
        search.onAction(SearchAction.ApplyFilters(laptop.copy(openOnly = true)))
        runCurrent()
        settle()
        assertEquals(laptop.copy(openOnly = true), map.state.value.filters)
        val edited = laptop.copy(tagIds = setOf("pet-friendly"), openOnly = true)
        search.onAction(SearchAction.ApplyFilters(edited))
        runCurrent()
        settle()
        assertEquals(edited, map.state.value.filters)
        assertEquals(edited, search.state.value.filters)
        assertEquals(2, analytics.filters.size)
    }

    @Test fun nearbyUsesSameUserOriginForCardsMarkersAndSuggestionsAfterCameraMoves() = runTest(dispatcher) {
        val vm = model()
        val origin = Coordinates(53.9, 27.56)
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria("coffee", laptop.copy(nearbyOnly = true), origin)))
        runCurrent()
        settle()
        vm.onAction(MapAction.BoundsChanged(MapBounds(52.0, 23.5, 52.2, 23.8), 15f))
        runCurrent()
        settle()
        val selection = repository.queries.single()
        assertNull(selection.filters.cityId)
        assertEquals(origin, selection.origin)
        repository.mapCriteria.forEach { criteria ->
            assertEquals(selection.copy(pageSize = 20), criteria.searchQuery())
        }
    }

    @Test fun accountSwitchClearsSelectionAndDiscardsOldPersonalResponses() = runTest(dispatcher) {
        val vm = model()
        val late = CompletableDeferred<Result<MapContent>>()
        repository.map = { _, _, _ -> withContext(NonCancellable) { late.await() } }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = laptop.copy(favoritesOnly = true))))
        runCurrent()
        settle()
        repository.map = { _, _, _ -> Result.success(MapContent()) }
        account.key.value = "two"
        runCurrent()
        settle()
        late.complete(Result.success(MapContent(listOf(marker("private-old")))))
        runCurrent()
        assertFalse(vm.state.value.filters.favoritesOnly)
        assertTrue(vm.state.value.markers.isEmpty())
        assertTrue(vm.state.value.nearby.isEmpty())
        assertNull(vm.state.value.selectedShop)
    }

    @Test fun logoutClearsLoadedPersonalCards() = runTest(dispatcher) {
        val vm = model()
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("old")))) }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = laptop.copy(visitedOnly = true))))
        runCurrent()
        settle()
        vm.onAction(MapAction.SelectMarker("old"))
        runCurrent()
        assertTrue(vm.state.value.cardDetails.isNotEmpty())
        repository.map = { _, _, _ -> Result.success(MapContent()) }
        account.key.value = null
        runCurrent()
        assertTrue(vm.state.value.cardDetails.isEmpty())
        assertTrue(vm.state.value.markers.isEmpty())
        assertNull(vm.state.value.selectedShop)
        assertFalse(vm.state.value.filters.visitedOnly)
    }

    @Test fun changedFavoritesInvalidateMembershipAndTheSelectedCard() = runTest(dispatcher) {
        val vm = model()
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("favorite")))) }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = laptop.copy(favoritesOnly = true))))
        runCurrent()
        settle()
        vm.onAction(MapAction.SelectMarker("favorite"))
        runCurrent()
        repository.map = { _, _, _ -> Result.success(MapContent()) }
        favorites.flow.value = setOf("another")
        runCurrent()
        assertNull(vm.state.value.selectedShop)
        assertTrue(vm.state.value.nearby.isEmpty())
        settle()
        assertTrue(vm.state.value.markers.isEmpty())
    }

    @Test fun oneQuerySurvivesListMapAndSuggestionSelection() = runTest(dispatcher) {
        repository.search = { page(listOf(shop("a").copy(coordinates = Coordinates(53.9, 27.56)))) }
        val map = model()
        val search = sharedSearch(map)
        search.onAction(SearchAction.Query("laptop"))
        runCurrent()
        settle()
        assertEquals("laptop", map.state.value.query)
        search.onAction(SearchAction.Query("coffee"))
        runCurrent()
        settle()
        map.onAction(MapAction.SelectSuggestion("a"))
        runCurrent()
        assertEquals("coffee", map.state.value.query)
        assertEquals("coffee", search.state.value.query)
        assertEquals("a", map.state.value.selectedShop?.id)
        assertTrue(repository.mapCriteria.takeLast(2).all { it.text == "coffee" })
    }

    @Test fun mapQuickPersonalActionsUseTheSameSignInGateAsList() = runTest(dispatcher) {
        account.key.value = null
        val map = model()
        val search = sharedSearch(map)
        val events = mutableListOf<SearchEvent>()
        backgroundScope.launch { search.events.take(2).toList(events) }
        search.onAction(SearchAction.ToggleFavorites)
        search.onAction(SearchAction.ToggleVisited)
        runCurrent()
        settle()
        assertEquals<List<SearchEvent>>(listOf(SearchEvent.SignInRequired, SearchEvent.SignInRequired), events)
        assertFalse(map.state.value.filters.favoritesOnly)
        assertFalse(map.state.value.filters.visitedOnly)
        assertTrue(repository.mapCriteria.none { it.filters.favoritesOnly || it.filters.visitedOnly })
    }

    private fun TestScope.sharedSearch(map: MapViewModel): SearchViewModel {
        val search = SearchViewModel(repository, favorites, city, location, account, analytics)
        store.put("search", search)
        backgroundScope.launch { search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) } }
        runCurrent()
        return search
    }

    @Test fun latePersonalSuggestionsCannotReappearAfterAccountSwitch() = runTest(dispatcher) {
        val vm = model()
        val late = CompletableDeferred<Result<Paged<ShopSummary>>>()
        repository.search = { withContext(NonCancellable) { late.await() } }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria("coffee", laptop.copy(visitedOnly = true))))
        runCurrent()
        settle()
        repository.search = { page(emptyList()) }
        account.key.value = "two"
        runCurrent()
        settle()
        late.complete(page(listOf(shop("old-private"))))
        runCurrent()
        assertTrue(vm.state.value.suggestions.isEmpty())
        assertFalse(vm.state.value.isSearchLoading)
    }

    @Test fun lateMapCatalogCannotOverwriteAlreadySharedCriteria() = runTest(dispatcher) {
        val ready = CompletableDeferred<Unit>()
        repository.catalogs = { ready.await(); Result.success(DiscoveryCatalogs(cities = listOf(minskCity))) }
        val vm = model()
        val chosen = DiscoveryCriteria("coffee", laptop.copy(cityId = "brest", openOnly = true))
        vm.onAction(MapAction.SyncCriteria(chosen))
        runCurrent()
        ready.complete(Unit)
        runCurrent()
        settle()
        assertEquals(chosen, vm.state.value.criteria)
        assertTrue(repository.mapCriteria.all { it == chosen })
    }

    @Test fun mapLocationCallbacksCannotReplaceTheSharedNearbyOrigin() = runTest(dispatcher) {
        val vm = model()
        val origin = Coordinates(53.9, 27.56)
        val criteria = DiscoveryCriteria(filters = laptop.copy(nearbyOnly = true), origin = origin)
        vm.onAction(MapAction.SyncCriteria(criteria))
        runCurrent()
        vm.onAction(MapAction.MyLocationFound(Coordinates(52.1, 23.7)))
        runCurrent()
        assertEquals(origin, vm.state.value.criteria.origin)
        assertEquals(Coordinates(52.1, 23.7), vm.state.value.camera?.target)
    }
}
