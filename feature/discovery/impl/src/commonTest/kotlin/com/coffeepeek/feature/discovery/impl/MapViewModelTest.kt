package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.rules.center
import com.coffeepeek.feature.discovery.domain.rules.expandedForPrefetch
import com.coffeepeek.feature.discovery.domain.rules.nearbyBounds
import com.coffeepeek.feature.discovery.impl.map.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val repository = FakeRepository()
    private val city = FakeCity()
    private val location = FakeLocation()
    private val details = FakeShopDetails()
    private val analytics = RecordingAnalytics()
    private val view = MapBounds(53.88, 27.50, 53.92, 27.60)
    private val here = Coordinates(53.9, 27.56)

    private fun TestScope.model(): MapViewModel =
        MapViewModel(repository, city, location, details, analytics, FakeAccount(), FakeFavorites()).also { store.put("map", it); it.onAction(MapAction.PresentationChanged(true)); runCurrent() }

    private fun TestScope.settle() { advanceTimeBy(300); runCurrent() }
    private fun MapViewModel.send(vararg actions: MapAction, scope: TestScope) { actions.forEach { onAction(it); scope.runCurrent() } }
    private val MapViewModel.now get() = state.value
    private fun queryAction(text: String) = MapAction.SyncCriteria(DiscoveryCriteria(text, DiscoveryFilters(cityId = "minsk")))
    private fun cityRequests() = repository.mapRequests.filter { it.second != 22f }

    // ---- the viewport ----

    @Test fun theFirstLoadWaitsForTheCatalogsAndUsesThePrefetchAreaAndTheCity() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<DiscoveryCatalogs>>()
        repository.catalogs = { gate.await() }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this); settle()
        assertTrue(repository.mapRequests.isEmpty())
        gate.complete(Result.success(DiscoveryCatalogs(cities = listOf(minskCity)))); runCurrent(); settle()
        val request = cityRequests().single()
        assertEquals(view.expandedForPrefetch(), request.first); assertEquals(14f, request.second); assertEquals("minsk", request.third.cityId)
    }

    @Test fun theBoundsAreDebouncedAndAPanInsideTheLoadedAreaLoadsNothing() = runTest(dispatcher) {
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("a")))) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), MapAction.BoundsChanged(MapBounds(53.881, 27.501, 53.921, 27.601), 14f), scope = this)
        advanceTimeBy(249); runCurrent(); assertTrue(cityRequests().isEmpty())
        advanceTimeBy(2); runCurrent()
        assertEquals(1, cityRequests().size); assertEquals(listOf("a"), vm.now.markers.map { it.id }); assertFalse(vm.now.isLoading)
        vm.send(MapAction.BoundsChanged(MapBounds(53.885, 27.505, 53.925, 27.605), 14f), scope = this); settle()
        assertEquals(1, cityRequests().size)
        vm.send(MapAction.BoundsChanged(MapBounds(53.885, 27.505, 53.925, 27.605), 15f), scope = this); settle()
        assertEquals(2, cityRequests().size)
    }

    @Test fun zonesSeenInTheCityAreKeptAndForgottenWhenTheCityChanges() = runTest(dispatcher) {
        fun zone(id: String) = MapZone(id, id, "", null, here, 300.0, 2)
        var answer = listOf(zone("z1"))
        repository.map = { _, _, _ -> Result.success(MapContent(zones = answer)) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 12f), scope = this); settle()
        answer = listOf(zone("z2"))
        vm.send(MapAction.SearchArea, scope = this); settle()
        assertEquals(setOf("z1", "z2"), vm.now.zones.map { it.id }.toSet())
        city.flow.value = "brest"; runCurrent(); settle()
        assertEquals(listOf("z2"), vm.now.zones.map { it.id })
    }

    @Test fun aFailedLoadOffersSearchingTheAreaAndARetryWorks() = runTest(dispatcher) {
        repository.map = { _, _, _ -> Result.failure(DiscoveryFailure(FailureKind.Other)) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this); settle()
        assertTrue(vm.now.showSearchArea); assertFalse(vm.now.isLoading); assertEquals(MapInteraction.LoadFailed, analytics.interactions.last())
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("a")))) }
        vm.send(MapAction.SearchArea, scope = this); settle()
        assertFalse(vm.now.showSearchArea); assertEquals(listOf("a"), vm.now.markers.map { it.id })
    }

    @Test fun aMovedViewReportsTheLoadAndTruncation() = runTest(dispatcher) {
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("a"), marker("b")), isTruncated = true)) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this); settle()
        assertTrue(vm.now.isTruncated); assertTrue(MapInteraction.Loaded in analytics.interactions)
    }

    @Test fun serverClustersReachTheCanvasAndDisappearWhenTheViewportUnfolds() = runTest(dispatcher) {
        val cluster = MapCluster("group", here, 12, view)
        repository.map = { _, zoom, _ ->
            Result.success(if (zoom == 22f) MapContent(listOf(marker("near"))) else MapContent(clusters = listOf(cluster)))
        }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 12f), scope = this)
        settle()
        assertEquals(listOf(cluster), vm.now.clusters)
        assertEquals(listOf("near"), vm.now.nearby.map { it.id })
        assertTrue(vm.now.markers.isEmpty())
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("unfolded")))) }
        vm.send(MapAction.BoundsChanged(view, 18f), scope = this)
        settle()
        assertTrue(vm.now.clusters.isEmpty())
        assertEquals(listOf("unfolded"), vm.now.markers.map { it.id })
    }

    // ---- nearby ----

    @Test fun theNearbyCarouselKeepsTheTenNearestWithoutDuplicatingClusteredPoints() = runTest(dispatcher) {
        repository.map = { bounds, zoom, _ ->
            if (zoom == 22f) Result.success(MapContent((1..12).map { marker("n$it", 53.9 + it * 0.001) } + marker("far", 54.5)))
            else Result.success(MapContent())
        }
        val vm = model()
        location.flow.value = here; runCurrent()
        assertEquals(nearbyBounds(here), repository.mapRequests.last().first)
        assertEquals((1..10).map { "n$it" }, vm.now.nearby.map { it.id }); assertTrue(vm.now.markers.isEmpty()); assertTrue(vm.now.hasCarousel)
    }

    @Test fun anInvalidPositionIsIgnoredAndTheFirstViewBecomesTheOrigin() = runTest(dispatcher) {
        val vm = model()
        location.flow.value = Coordinates(91.0, 27.0); runCurrent()
        assertNull(vm.now.nearbyOrigin)
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this)
        assertEquals(view.center, vm.now.nearbyOrigin)
    }

    // ---- selection ----

    @Test fun aLateDetailsAnswerForAnotherSelectionIsDroppedAndADroppedOneIsAskedAgainButACachedOneIsNot() = runTest(dispatcher) {
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("a"), marker("b")))) }
        val slow = CompletableDeferred<Unit>()
        details.answer = { id -> if (id == "a") slow.await(); Result.success(sampleDetails(id)) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this); settle()
        vm.send(MapAction.SelectMarker("a"), MapAction.SelectMarker("b"), scope = this)
        assertEquals("photo-b", vm.now.details?.photoUrl)
        slow.complete(Unit); runCurrent()
        assertEquals("b", vm.now.selectedShop?.id); assertEquals("photo-b", vm.now.details?.photoUrl)
        vm.send(MapAction.SelectMarker("a"), scope = this)
        assertEquals("photo-a", vm.now.details?.photoUrl); assertFalse(vm.now.isLoadingDetails)
        vm.send(MapAction.SelectMarker("b"), scope = this)
        assertEquals(listOf("a", "b", "a"), details.requests)
    }

    @Test fun aFailedDetailsLoadStopsTheLoadingStateAndTheMarkerStaysSelected() = runTest(dispatcher) {
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("a")))) }
        details.answer = { Result.failure(IllegalStateException("offline")) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this); settle()
        vm.send(MapAction.SelectMarker("a"), scope = this)
        assertEquals("a", vm.now.selectedShop?.id); assertFalse(vm.now.isLoadingDetails); assertNull(vm.now.details)
    }

    @Test fun aCarouselCardSelectsTheShopAndMovesTheCameraToZoom16() = runTest(dispatcher) {
        repository.map = { _, zoom, _ -> Result.success(MapContent(if (zoom == 22f) listOf(marker("n1")) else emptyList())) }
        val vm = model()
        location.flow.value = here; runCurrent()
        vm.send(MapAction.SelectFromCarousel("n1"), scope = this)
        assertEquals("n1", vm.now.selectedShop?.id); assertEquals(16f, vm.now.camera?.zoom); assertEquals(marker("n1").coordinates, vm.now.camera?.target)
        vm.send(MapAction.CameraApplied, scope = this); assertNull(vm.now.camera)
        assertEquals(MapInteraction.MarkerSelected, analytics.interactions.last())
    }

    @Test fun focusingAShopAddsItSelectsItAndMovesTheCamera() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.Focus(MapFocus("x", "Shop x", here)), scope = this)
        assertEquals("x", vm.now.selectedShop?.id); assertTrue(vm.now.markers.any { it.id == "x" }); assertEquals(16f, vm.now.camera?.zoom)
    }

    // ---- zones ----

    @Test fun aZoneIsSelectedZoomsInOnRequestAndTheSwitchHidesTheSelection() = runTest(dispatcher) {
        val zone = MapZone("z", "Zone", "d", null, here, 300.0, 4)
        repository.map = { _, _, _ -> Result.success(MapContent(zones = listOf(zone))) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 12f), scope = this); settle()
        vm.send(MapAction.SelectZone("z"), scope = this)
        assertEquals(zone, vm.now.selectedZone)
        vm.send(MapAction.ShowZoneShops, scope = this)
        assertNull(vm.now.selectedZone); assertEquals(14.5f, vm.now.camera?.zoom); assertEquals(here, vm.now.camera?.target)
        vm.send(MapAction.SelectZone("z"), MapAction.ToggleZones, scope = this)
        assertFalse(vm.now.showZones); assertNull(vm.now.selectedZone); assertEquals(MapInteraction.ZonesToggled, analytics.interactions.last())
    }

    // ---- the camera ----

    @Test fun zoomMovesTheCameraToTheCenterOfTheViewAndStaysWithinTheLimits() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.ZoomIn, scope = this)
        assertNull(vm.now.camera)
        vm.send(MapAction.BoundsChanged(view, 19.5f), scope = this)
        vm.send(MapAction.ZoomIn, scope = this)
        assertEquals(20f, vm.now.camera?.zoom); assertEquals(view.center, vm.now.camera?.target)
        vm.send(MapAction.CameraApplied, MapAction.ZoomIn, scope = this); assertNull(vm.now.camera)
        vm.send(MapAction.BoundsChanged(view, 3.4f), MapAction.ZoomOut, scope = this)
        assertEquals(3f, vm.now.camera?.zoom)
    }

    @Test fun viewportChangesDuringACameraMoveAreRecordedAndLoadedAfterThePause() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), scope = this); settle()
        val before = cityRequests().size
        vm.send(MapAction.Focus(MapFocus("x", "Shop x", here)), scope = this)
        val moved = MapBounds(50.0, 20.0, 50.1, 20.1)
        vm.send(MapAction.BoundsChanged(moved, 16f), scope = this)
        advanceTimeBy(600); runCurrent(); assertEquals(before, cityRequests().size)
        advanceTimeBy(200); runCurrent(); settle()
        assertEquals(before + 1, cityRequests().size); assertEquals(moved.expandedForPrefetch(), cityRequests().last().first)
    }

    @Test fun myLocationIsAskedFoundAndReportedWithItsTrigger() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.LocationDenied, scope = this)
        vm.send(MapAction.RequestMyLocation, scope = this)
        assertEquals(0, vm.now.myLocationRequest)
        assertEquals(1, vm.now.locationPermissionRequest)
        vm.send(MapAction.LocationGranted, scope = this)
        assertEquals(1, vm.now.myLocationRequest)
        assertEquals(1, location.refreshes)
        vm.send(MapAction.MyLocationFound(here), scope = this)
        assertEquals(15f, vm.now.camera?.zoom); assertEquals(here, vm.now.nearbyOrigin)
        assertEquals(listOf(Triple("map", false, GeolocationOutcome.Unavailable), Triple("map", true, GeolocationOutcome.Available)), analytics.geolocations)
    }

    @Test fun openingAndLoadingTheMapDoesNotRequestLocationPermission() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), MapAction.CameraApplied, scope = this)
        settle()
        vm.send(MapAction.Focus(MapFocus("shop", "Shop", here)), scope = this)
        assertEquals(0, vm.now.locationPermissionRequest)
        assertEquals(0, vm.now.myLocationRequest)
        assertEquals(0, location.refreshes)
    }

    @Test fun mapInteractionsAskOnceAndKeepZoomWorkingAfterDenial() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), MapAction.Interact, MapAction.Interact, scope = this)
        assertEquals(1, vm.now.locationPermissionRequest)
        vm.send(MapAction.LocationDenied, MapAction.Interact, MapAction.ZoomIn, MapAction.ZoomOut, scope = this)
        assertEquals(1, vm.now.locationPermissionRequest)
        assertEquals(14f, vm.now.camera?.zoom)
        assertEquals(0, vm.now.myLocationRequest)
        assertEquals(listOf(Triple("map", true, GeolocationOutcome.Unavailable)), analytics.geolocations)
    }

    @Test fun eitherZoomButtonCanBeTheFirstPermissionTrigger() = runTest(dispatcher) {
        for (action in listOf(MapAction.ZoomIn, MapAction.ZoomOut)) {
            val vm = model()
            vm.send(action, scope = this)
            assertEquals(1, vm.now.locationPermissionRequest)
        }
    }

    @Test fun grantingPermissionAfterZoomRefreshesNearbyWithoutRecentering() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), MapAction.ZoomIn, scope = this)
        val camera = vm.now.camera
        vm.send(MapAction.LocationGranted, scope = this)
        assertEquals(1, location.refreshes)
        assertEquals(0, vm.now.myLocationRequest)
        assertEquals(camera, vm.now.camera)
        assertEquals(listOf(Triple("map", true, GeolocationOutcome.Available)), analytics.geolocations)
    }

    @Test fun locationClicksShareThePendingRequestAndDenialNeverStartsNativeLocation() = runTest(dispatcher) {
        val vm = model()
        vm.send(MapAction.Interact, MapAction.RequestMyLocation, MapAction.RequestMyLocation, scope = this)
        assertEquals(1, vm.now.locationPermissionRequest)
        vm.send(MapAction.LocationDenied, scope = this)
        assertEquals(0, vm.now.myLocationRequest)
        vm.send(MapAction.RequestMyLocation, MapAction.LocationGranted, scope = this)
        assertEquals(2, vm.now.locationPermissionRequest)
        assertEquals(1, vm.now.myLocationRequest)
    }

    // ---- the search ----

    @Test fun theSearchIsDebouncedTrimmedAndRetainsCardsWithoutCoordinates() = runTest(dispatcher) {
        repository.search = { q -> page(listOf(shop("a").copy(coordinates = here), shop("b"))) }
        val vm = model()
        vm.send(queryAction("c"), queryAction("  cof "), scope = this)
        assertTrue(vm.now.showSuggestions); assertTrue(vm.now.isSearchLoading); assertTrue(repository.queries.isEmpty())
        advanceTimeBy(351); runCurrent()
        val query = repository.queries.single()
        assertEquals("cof", query.text); assertEquals(3, query.pageSize); assertEquals("minsk", query.filters.cityId)
        assertEquals(listOf("a", "b"), vm.now.suggestions.map { it.id }); assertFalse(vm.now.isSearchLoading); assertEquals(listOf(2), analytics.mapSearchResults)
    }

    @Test fun aCardWithoutCoordinatesResolvesItsDetailsOnSelectionAndReusesTheCache() = runTest(dispatcher) {
        repository.search = { page(listOf(shop("a"))) }
        details.answer = { Result.success(sampleDetails(it).copy(coordinates = here)) }
        val vm = model()
        vm.send(queryAction("a"), scope = this)
        advanceTimeBy(351)
        runCurrent()
        vm.send(MapAction.SelectSuggestion("a"), scope = this)
        assertEquals(here, vm.now.camera?.target)
        assertEquals("a", vm.now.selectedShop?.id)
        assertEquals(listOf("a"), details.requests)
        assertFalse(vm.now.showSuggestions)
        assertFalse(vm.now.isSearchLoading)
    }

    @Test fun missingCoordinatesOrFailedDetailsKeepSuggestionsAvailableForRetry() = runTest(dispatcher) {
        repository.search = { page(listOf(shop("a"))) }
        val vm = model()
        vm.send(queryAction("a"), scope = this)
        advanceTimeBy(351)
        runCurrent()
        vm.send(MapAction.SelectSuggestion("a"), scope = this)
        assertTrue(vm.now.searchFailed)
        assertEquals("a", vm.now.query)
        assertEquals(listOf("a"), vm.now.suggestions.map { it.id })
        assertNull(vm.now.camera)
        details.answer = { Result.failure(IllegalStateException("offline")) }
        vm.send(MapAction.SelectSuggestion("a"), scope = this)
        assertTrue(vm.now.searchFailed)
        assertFalse(vm.now.isSearchLoading)
    }

    @Test fun changingQueryCancelsAnUnlocatedSuggestionSelection() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.search = { page(listOf(shop("a"))) }
        details.answer = { gate.await(); Result.success(sampleDetails(it).copy(coordinates = here)) }
        val vm = model()
        vm.send(queryAction("a"), scope = this)
        advanceTimeBy(351)
        runCurrent()
        vm.send(MapAction.SelectSuggestion("a"), queryAction("new"), scope = this)
        gate.complete(Unit)
        runCurrent()
        assertEquals("new", vm.now.query)
        assertNull(vm.now.selectedShop)
        assertNull(vm.now.camera)
    }

    @Test fun aStaleSearchAnswerIsIgnoredAndAFailureIsShown() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.search = { q -> if (q.text == "old") gate.await(); page(listOf(shop(q.text!!).copy(coordinates = here))) }
        val vm = model()
        vm.send(queryAction("old"), scope = this); advanceTimeBy(351); runCurrent()
        vm.send(queryAction("new"), scope = this); advanceTimeBy(351); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("new"), vm.now.suggestions.map { it.id })
        repository.search = { Result.failure(DiscoveryFailure(FailureKind.Other)) }
        vm.send(queryAction("bad"), scope = this); advanceTimeBy(351); runCurrent()
        assertTrue(vm.now.searchFailed); assertFalse(vm.now.isSearchLoading); assertEquals(1, analytics.mapSearchFailures.size)
    }

    @Test fun aPickedSuggestionPreservesTheSearchSelectsTheShopAndTheOpeningIsReportedAsFromSearch() = runTest(dispatcher) {
        repository.search = { page(listOf(shop("a").copy(coordinates = here))) }
        val vm = model()
        vm.send(queryAction("a"), scope = this); advanceTimeBy(351); runCurrent()
        vm.send(MapAction.SelectSuggestion("a"), scope = this)
        assertEquals("a", vm.now.query); assertFalse(vm.now.showSuggestions); assertEquals("a", vm.now.selectedShop?.id)
        assertEquals(16f, vm.now.camera?.zoom); assertEquals(MapInteraction.SearchSelected, analytics.interactions.last())
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.events.first() }
        vm.send(MapAction.OpenShop("a"), scope = this)
        assertEquals(MapEvent.OpenShop("a"), event.await()); assertEquals(listOf(true), analytics.mapOpened)
    }

    @Test fun selectingAMarkerPreservesTheQueryAndReportsTheOpeningAsNotFromSearch() = runTest(dispatcher) {
        repository.map = { _, _, _ -> Result.success(MapContent(listOf(marker("a")))) }
        val vm = model()
        vm.send(MapAction.BoundsChanged(view, 14f), queryAction("zz"), scope = this); settle()
        vm.send(MapAction.SelectMarker("a"), scope = this)
        assertEquals("zz", vm.now.query); assertFalse(vm.now.showSuggestions)
        vm.send(MapAction.OpenShop("a"), scope = this)
        assertEquals(listOf(false), analytics.mapOpened)
    }
}
