package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyRequestTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private val city = FakeCity()
    private val location = FakeLocation()
    private val analytics = RecordingAnalytics()
    private val here = Coordinates(53.9, 27.56)
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model(): SearchViewModel {
        val model = SearchViewModel(repository, FakeFavorites(), city, location, FakeAccount(), analytics)
        store.put("nearby", model)
        runCurrent()
        return model
    }

    @Test fun onlyATapRequestsPermissionAndDuplicateTapsDoNotRepeatIt() = runTest(dispatcher) {
        val vm = model()
        assertEquals(0, vm.state.value.locationPermissionRequest)
        vm.onAction(SearchAction.ToggleNearby)
        vm.onAction(SearchAction.ToggleNearby)
        runCurrent()
        assertEquals(1, vm.state.value.locationPermissionRequest)
        assertTrue(vm.state.value.nearbyRequested)
        assertFalse(vm.state.value.filters.nearbyOnly)
        assertEquals(1, repository.queries.size)
    }

    @Test fun grantWaitsForValidCoordinatesThenUsesNearbyInsteadOfCity() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(SearchAction.ToggleNearby)
        vm.onAction(SearchAction.LocationGranted)
        runCurrent()
        assertEquals(1, location.refreshes)
        assertFalse(vm.state.value.filters.nearbyOnly)
        location.flow.value = Coordinates(91.0, 27.0)
        runCurrent()
        assertFalse(vm.state.value.filters.nearbyOnly)
        location.flow.value = here
        runCurrent()
        assertTrue(vm.state.value.filters.nearbyOnly)
        assertFalse(vm.state.value.nearbyRequested)
        assertNull(repository.queries.last().filters.cityId)
        assertEquals(here, repository.queries.last().origin)
        assertEquals(Triple("discovery", true, GeolocationOutcome.Available), analytics.geolocations.last())
    }

    @Test fun refusalAndRepeatedRefusalKeepTheCityResultsAndAllowAnotherExplicitAttempt() = runTest(dispatcher) {
        val vm = model()
        val shops = vm.state.value.shops
        repeat(2) {
            vm.onAction(SearchAction.ToggleNearby)
            vm.onAction(SearchAction.LocationDenied)
            runCurrent()
            assertFalse(vm.state.value.nearbyRequested)
            assertTrue(vm.state.value.locationUnavailable)
            assertSame(shops, vm.state.value.shops)
        }
        assertEquals(2, vm.state.value.locationPermissionRequest)
        assertEquals("minsk", vm.state.value.filters.cityId)
        assertEquals(1, repository.queries.size)
        assertEquals(List(2) { Triple("discovery", true, GeolocationOutcome.Unavailable) }, analytics.geolocations)
    }

    @Test fun aGrantedPermissionWithoutAPositionTimesOutWithoutChangingResults() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(SearchAction.ToggleNearby)
        vm.onAction(SearchAction.LocationGranted)
        runCurrent()
        advanceTimeBy(10_001)
        runCurrent()
        assertFalse(vm.state.value.nearbyRequested)
        assertTrue(vm.state.value.locationUnavailable)
        assertFalse(vm.state.value.filters.nearbyOnly)
        assertEquals(1, repository.queries.size)
    }

    @Test fun changingCityCancelsPendingNearbyAndALatePositionCannotEnableIt() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(SearchAction.ToggleNearby)
        vm.onAction(SearchAction.LocationGranted)
        runCurrent()
        city.flow.value = "brest"
        runCurrent()
        location.flow.value = here
        runCurrent()
        advanceTimeBy(10_001)
        runCurrent()
        assertFalse(vm.state.value.nearbyRequested)
        assertFalse(vm.state.value.locationUnavailable)
        assertFalse(vm.state.value.filters.nearbyOnly)
        assertEquals("brest", repository.queries.last().filters.cityId)
    }

    @Test fun changingCityLeavesAnActiveNearbySearchAndPreservesOtherFilters() = runTest(dispatcher) {
        val vm = model()
        location.flow.value = here
        runCurrent()
        vm.onAction(SearchAction.ToggleNearby)
        vm.onAction(SearchAction.ToggleOpen)
        runCurrent()
        city.flow.value = "brest"
        runCurrent()
        assertFalse(vm.state.value.filters.nearbyOnly)
        assertTrue(vm.state.value.filters.openOnly)
        assertEquals("brest", repository.queries.last().filters.cityId)
    }

    @Test fun dismissingPendingRequestDoesNotEnableNearbyAfterLateGrant() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(SearchAction.ToggleNearby)
        vm.onAction(SearchAction.DismissLocationFailure)
        vm.onAction(SearchAction.LocationGranted)
        runCurrent()
        location.flow.value = here
        runCurrent()
        assertFalse(vm.state.value.filters.nearbyOnly)
        assertFalse(vm.state.value.locationUnavailable)
        assertEquals(1, repository.queries.size)
    }
}
