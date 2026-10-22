package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.api.SettingsAccountPort
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.api.UpdateCheckPort
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.domain.UpdateCheckState
import com.coffeepeek.feature.settings.impl.ui.model.SettingsAction
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val minsk = SettingsCity("minsk", "Минск")
    private val brest = SettingsCity("brest", "Брест")

    private class Account(initial: Boolean) : SettingsAccountPort {
        val signedIn = MutableStateFlow(initial)
        override fun isSignedIn() = signedIn.value
        override fun observeSignedIn(): Flow<Boolean> = signedIn
    }

    private class Theme(initial: ThemeMode) : ThemePort {
        val mode = MutableStateFlow(initial)
        override fun current() = mode.value
        override fun observe(): Flow<ThemeMode> = mode
        override fun set(mode: ThemeMode) { this.mode.value = mode }
    }

    private class Cities(var result: Result<List<SettingsCity>>) : CityPort {
        val selected = MutableStateFlow<String?>(null)
        var loads = 0
        override fun observeSelectedCityId(): Flow<String?> = selected
        override suspend fun loadCities(): Result<List<SettingsCity>> {
            loads++
            result.onSuccess { list -> if (selected.value == null) selected.value = list.firstOrNull()?.id }
            return result
        }
        override fun select(cityId: String) { selected.value = cityId }
    }

    private class Updates : UpdateCheckPort {
        val state = MutableStateFlow(UpdateCheckState())
        var checks = 0
        override fun observe(): Flow<UpdateCheckState> = state
        override fun check() { checks++ }
    }

    private fun model(account: Account = Account(true), theme: Theme = Theme(ThemeMode.SYSTEM), cities: Cities = Cities(Result.success(listOf(minsk, brest))),
        updates: Updates = Updates()) = SettingsViewModel(account, theme, cities, updates).also { store.put("settings", it) }

    @Test fun theTabStartsOnTheCurrentAccountAndThemeBeforeAnythingEmits() = runTest(dispatcher) {
        val vm = model(Account(false), Theme(ThemeMode.DARK))
        assertFalse(vm.state.value.signedIn)
        assertEquals(ThemeMode.DARK, vm.state.value.theme)
        assertNull(vm.state.value.cityName)
    }

    @Test fun rowRulesFollowTheSession() = runTest(dispatcher) {
        val account = Account(false); val vm = model(account); runCurrent()
        assertTrue(vm.state.value.showGuestCard); assertTrue(vm.state.value.showAddRows)
        account.signedIn.value = true; runCurrent()
        assertFalse(vm.state.value.showGuestCard); assertTrue(vm.state.value.showAddRows)
    }

    @Test fun theCatalogIsLoadedOnCreationAndTheSelectedCityNameIsShown() = runTest(dispatcher) {
        val cities = Cities(Result.success(listOf(minsk, brest))); val vm = model(cities = cities); runCurrent()
        assertEquals(1, cities.loads)
        assertEquals("Минск", vm.state.value.cityName)
        cities.selected.value = "brest"; runCurrent()
        assertEquals("Брест", vm.state.value.cityName)
        assertEquals(1, cities.loads)
    }

    @Test fun aFailedCatalogLeavesTheRowOnChooseUntilASelectionNeedsAReload() = runTest(dispatcher) {
        val cities = Cities(Result.failure(IllegalStateException("offline"))); val vm = model(cities = cities); runCurrent()
        assertNull(vm.state.value.cityName)
        // The city screen got the catalog later and selected a city: the tab asks for the list again and names it.
        cities.result = Result.success(listOf(minsk, brest))
        cities.selected.value = "brest"; runCurrent()
        assertEquals(2, cities.loads)
        assertEquals("Брест", vm.state.value.cityName)
    }

    @Test fun theThemeValueFollowsChangesMadeOnTheThemeScreen() = runTest(dispatcher) {
        val theme = Theme(ThemeMode.SYSTEM); val vm = model(theme = theme); runCurrent()
        theme.mode.value = ThemeMode.LIGHT; runCurrent()
        assertEquals(ThemeMode.LIGHT, vm.state.value.theme)
    }

    @Test fun theUpdateRowMirrorsThePortAndACheckCallsItOnce() = runTest(dispatcher) {
        val updates = Updates(); val vm = model(updates = updates); runCurrent()
        vm.onAction(SettingsAction.CheckForUpdates); runCurrent()
        assertEquals(1, updates.checks)
        updates.state.value = UpdateCheckState(checking = true); runCurrent()
        assertTrue(vm.state.value.update.checking)
        updates.state.value = UpdateCheckState(message = "Установлена актуальная версия"); runCurrent()
        assertEquals("Установлена актуальная версия", vm.state.value.update.message)
        assertFalse(vm.state.value.update.checking)
    }
}
