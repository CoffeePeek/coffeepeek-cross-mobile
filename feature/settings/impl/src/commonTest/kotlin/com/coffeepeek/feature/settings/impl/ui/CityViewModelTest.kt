package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.impl.ui.model.CityAction
import com.coffeepeek.feature.settings.impl.ui.model.CityLoad
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val minsk = SettingsCity("minsk", "Минск")
    private val brest = SettingsCity("brest", "Брест")

    private class FakeCities(var result: Result<List<SettingsCity>>, var gate: CompletableDeferred<Unit>? = null) : CityPort {
        val selected = MutableStateFlow<String?>(null)
        val selections = mutableListOf<String>()
        var loads = 0
        override fun observeSelectedCityId(): Flow<String?> = selected
        override suspend fun loadCities(): Result<List<SettingsCity>> {
            loads++; gate?.await()
            // As the app port does: a successful load resolves a missing choice to the first city.
            result.onSuccess { list -> if (selected.value == null) selected.value = list.firstOrNull()?.id }
            return result
        }
        override fun select(cityId: String) { selections += cityId; selected.value = cityId }
    }

    private fun model(port: FakeCities) = CityViewModel(port).also { store.put("city", it) }

    @Test fun theListLoadsOnOpenAndShowsTheResolvedSelection() = runTest(dispatcher) {
        val port = FakeCities(Result.success(listOf(minsk, brest)))
        val vm = model(port)
        assertEquals(CityLoad.Loading, vm.state.value.load)
        runCurrent()
        assertEquals(CityLoad.Loaded, vm.state.value.load)
        assertEquals(listOf(minsk, brest), vm.state.value.cities)
        assertEquals("minsk", vm.state.value.selectedId)
    }

    @Test fun onlyListedIdsCanBeSelectedAndTheChoiceIsShown() = runTest(dispatcher) {
        val port = FakeCities(Result.success(listOf(minsk, brest))); val vm = model(port); runCurrent()
        vm.onAction(CityAction.Select("nowhere")); runCurrent()
        assertEquals(emptyList(), port.selections)
        vm.onAction(CityAction.Select("brest")); runCurrent()
        assertEquals(listOf("brest"), port.selections)
        assertEquals("brest", vm.state.value.selectedId)
    }

    @Test fun aFailedLoadIsAnErrorWithARetryThatLoadsAgain() = runTest(dispatcher) {
        val port = FakeCities(Result.failure(IllegalStateException("offline"))); val vm = model(port); runCurrent()
        assertEquals(CityLoad.Failed, vm.state.value.load)
        assertTrue(vm.state.value.cities.isEmpty())
        port.result = Result.success(listOf(minsk))
        vm.onAction(CityAction.Load); runCurrent()
        assertEquals(CityLoad.Loaded, vm.state.value.load)
        assertEquals(listOf(minsk), vm.state.value.cities)
        assertEquals(2, port.loads)
    }

    @Test fun anEmptyCatalogIsLoadedButEmptyAndRetryAsksAgain() = runTest(dispatcher) {
        val port = FakeCities(Result.success(emptyList())); val vm = model(port); runCurrent()
        assertEquals(CityLoad.Loaded, vm.state.value.load); assertTrue(vm.state.value.cities.isEmpty())
        vm.onAction(CityAction.Load); runCurrent()
        assertEquals(2, port.loads)
    }

    @Test fun aLoadedListIsNotReloadedAndTwoRetriesInTheSameFrameLoadOnce() = runTest(dispatcher) {
        val port = FakeCities(Result.success(listOf(minsk))); val vm = model(port); runCurrent()
        vm.onAction(CityAction.Load); runCurrent()
        assertEquals(1, port.loads)

        val gate = CompletableDeferred<Unit>()
        val slow = FakeCities(Result.failure(IllegalStateException("x")), gate); val second = model(slow)
        runCurrent()
        slow.result = Result.success(listOf(minsk))
        second.onAction(CityAction.Load); second.onAction(CityAction.Load); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(1, slow.loads)
    }
}
