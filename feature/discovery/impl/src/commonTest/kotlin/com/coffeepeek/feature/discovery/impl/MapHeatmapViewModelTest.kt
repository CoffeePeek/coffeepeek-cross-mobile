package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.impl.map.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapHeatmapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private val analytics = RecordingAnalytics()
    private val account = FakeAccount()
    private val bounds = MapBounds(53.88, 27.50, 53.92, 27.60)
    private val heatmap = CheckInHeatmap(listOf(CheckInHeatPoint("a", Coordinates(53.9, 27.56), 20)))

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model(): MapViewModel {
        repository.heatmap = { _, _ -> Result.success(heatmap) }
        return MapViewModel(repository, FakeLocation(), FakeShopDetails(), analytics, account, FakeFavorites())
            .also { store.put("map", it); it.onAction(MapAction.BoundsChanged(bounds, 12f)); runCurrent() }
    }

    private fun TestScope.settle() { advanceTimeBy(600); runCurrent() }

    @Test fun guestCannotRequestPersonalizedHeatmapSelection() = runTest(dispatcher) {
        account.key.value = null
        val vm = model()
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria(filters = DiscoveryFilters(visitedOnly = true))))
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        assertTrue(repository.heatmapRequests.isEmpty())
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }

    @Test fun layerIsOptInAndZonesAndHeatmapAreMutuallyExclusive() = runTest(dispatcher) {
        val vm = model()
        settle()
        assertTrue(repository.heatmapRequests.isEmpty())
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        assertTrue(vm.state.value.showHeatmap)
        assertFalse(vm.state.value.showZones)
        assertEquals(heatmap, vm.state.value.heatmap)
        assertTrue(MapInteraction.HeatmapLoaded in analytics.interactions)
        vm.onAction(MapAction.ToggleZones)
        runCurrent()
        assertFalse(vm.state.value.showHeatmap)
        assertTrue(vm.state.value.showZones)
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }

    @Test fun togglingOffRejectsALateNonCooperativeReply() = runTest(dispatcher) {
        val vm = model()
        val gate = CompletableDeferred<Result<CheckInHeatmap>>()
        repository.heatmap = { _, _ -> withContext(NonCancellable) { gate.await() } }
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        vm.onAction(MapAction.ToggleHeatmap)
        runCurrent()
        gate.complete(Result.success(heatmap))
        runCurrent()
        assertFalse(vm.state.value.showHeatmap)
        assertFalse(vm.state.value.isHeatmapLoading)
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }

    @Test fun failedHeatmapCanRetryWithoutMovingTheCamera() = runTest(dispatcher) {
        val vm = model()
        repository.heatmap = { _, _ -> Result.failure(IllegalStateException("offline")) }
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        assertTrue(vm.state.value.heatmapFailed)
        assertFalse(vm.state.value.isHeatmapLoading)
        repository.heatmap = { _, _ -> Result.success(heatmap) }
        vm.onAction(MapAction.ToggleHeatmap)
        runCurrent()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        assertFalse(vm.state.value.heatmapFailed)
        assertEquals(heatmap, vm.state.value.heatmap)
        assertEquals(2, repository.heatmapRequests.size)
    }

    @Test fun filterChangesClearOldHeatAndRejectTheOldRequest() = runTest(dispatcher) {
        val vm = model()
        val gate = CompletableDeferred<Result<CheckInHeatmap>>()
        repository.heatmap = { _, _ -> withContext(NonCancellable) { gate.await() } }
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        repository.heatmap = { _, _ -> Result.success(CheckInHeatmap()) }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria("tea", DiscoveryFilters(cityId = "brest"))))
        settle()
        gate.complete(Result.success(heatmap))
        runCurrent()
        assertTrue(vm.state.value.heatmap.points.isEmpty())
        assertNull(repository.heatmapRequests.last().second.filters.cityId)
        assertEquals("tea", repository.heatmapRequests.last().second.text)
        assertFalse(vm.state.value.isHeatmapLoading)
    }

    @Test fun panningAndZoomButtonsInsidePrefetchKeepHeatWithoutReloading() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        vm.onAction(MapAction.BoundsChanged(bounds, 12.1f))
        settle()
        assertEquals(1, repository.heatmapRequests.size)
        assertEquals(heatmap, vm.state.value.heatmap)
        vm.onAction(MapAction.ZoomIn)
        runCurrent()
        assertEquals(heatmap, vm.state.value.heatmap)
        settle()
        settle()
        assertEquals(1, repository.heatmapRequests.size)
        vm.onAction(MapAction.ZoomOut)
        runCurrent()
        assertEquals(heatmap, vm.state.value.heatmap)
        settle()
        assertEquals(1, repository.heatmapRequests.size)
    }

    @Test fun changingAccountDropsPendingHeatAndLoadsTheNewSelection() = runTest(dispatcher) {
        val vm = model()
        val gate = CompletableDeferred<Result<CheckInHeatmap>>()
        repository.heatmap = { _, _ -> withContext(NonCancellable) { gate.await() } }
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        repository.heatmap = { _, _ -> Result.success(CheckInHeatmap()) }
        account.key.value = "two"
        settle()
        gate.complete(Result.success(heatmap))
        runCurrent()
        assertEquals(2, repository.heatmapRequests.size)
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }

    @Test fun movingOutsideTheLoadedAreaKeepsHeatUntilSuccessfulEmptyResult() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        val gate = CompletableDeferred<Result<CheckInHeatmap>>()
        repository.heatmap = { _, _ -> gate.await() }
        vm.onAction(MapAction.BoundsChanged(MapBounds(52.0, 23.0, 52.1, 23.1), 12f))
        settle()
        assertTrue(vm.state.value.isHeatmapLoading)
        assertEquals(heatmap, vm.state.value.heatmap)
        gate.complete(Result.success(CheckInHeatmap()))
        runCurrent()
        assertFalse(vm.state.value.isHeatmapLoading)
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }

    @Test fun zoomingOutKeepsRenderedHeatUntilNewCountsArrive() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        val rendered = vm.state.value.canvasHeatmap(false)
        val gate = CompletableDeferred<Result<CheckInHeatmap>>()
        repository.heatmap = { _, _ -> gate.await() }
        vm.onAction(MapAction.BoundsChanged(MapBounds(53.7, 27.3, 54.1, 27.8), 10f))
        settle()
        assertTrue(vm.state.value.isHeatmapLoading)
        assertEquals(rendered, vm.state.value.canvasHeatmap(false))
        val updated = heatmap.copy(points = heatmap.points.map { it.copy(checkInCount = 40) })
        gate.complete(Result.success(updated))
        runCurrent()
        assertEquals(updated, vm.state.value.heatmap)
        assertFalse(vm.state.value.isHeatmapLoading)
    }

    @Test fun failedViewportRefreshKeepsPreviouslyLoadedHeat() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        repository.heatmap = { _, _ -> Result.failure(IllegalStateException("offline")) }
        vm.onAction(MapAction.BoundsChanged(MapBounds(53.7, 27.3, 54.1, 27.8), 10f))
        settle()
        assertTrue(vm.state.value.heatmapFailed)
        assertEquals(heatmap, vm.state.value.heatmap)
        assertFalse(vm.state.value.isHeatmapLoading)
    }

    @Test fun changingFiltersImmediatelyClearsPreviouslyRenderedHeat() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        assertEquals(heatmap, vm.state.value.heatmap)
        repository.heatmap = { _, _ -> Result.success(CheckInHeatmap()) }
        vm.onAction(MapAction.SyncCriteria(DiscoveryCriteria("tea")))
        runCurrent()
        assertTrue(vm.state.value.heatmap.points.isEmpty())
        settle()
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }

    @Test fun changingAccountsImmediatelyClearsPreviouslyRenderedHeat() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(MapAction.ToggleHeatmap)
        settle()
        assertEquals(heatmap, vm.state.value.heatmap)
        repository.heatmap = { _, _ -> Result.success(CheckInHeatmap()) }
        account.key.value = "two"
        runCurrent()
        assertTrue(vm.state.value.heatmap.points.isEmpty())
        settle()
        assertTrue(vm.state.value.heatmap.points.isEmpty())
    }
}
