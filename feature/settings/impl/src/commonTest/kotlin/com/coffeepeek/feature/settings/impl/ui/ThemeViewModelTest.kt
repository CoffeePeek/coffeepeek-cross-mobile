package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.impl.ui.model.ThemeAction
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

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class FakeTheme(initial: ThemeMode) : ThemePort {
        val mode = MutableStateFlow(initial)
        val chosen = mutableListOf<ThemeMode>()
        override fun current() = mode.value
        override fun observe(): Flow<ThemeMode> = mode
        override fun set(mode: ThemeMode) { chosen += mode; this.mode.value = mode }
    }

    private fun model(theme: FakeTheme) = ThemeViewModel(theme).also { store.put("theme", it) }

    @Test fun theScreenStartsOnTheCurrentModeEvenBeforeTheFlowEmits() = runTest(dispatcher) {
        val vm = model(FakeTheme(ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, vm.state.value.selected)
    }

    @Test fun aChoiceIsAppliedImmediatelyThroughThePortAndShownOnce() = runTest(dispatcher) {
        val theme = FakeTheme(ThemeMode.SYSTEM); val vm = model(theme); runCurrent()
        vm.onAction(ThemeAction.Select(ThemeMode.LIGHT)); runCurrent()
        assertEquals(listOf(ThemeMode.LIGHT), theme.chosen)
        assertEquals(ThemeMode.LIGHT, vm.state.value.selected)
    }

    @Test fun aChangeMadeElsewhereIsFollowed() = runTest(dispatcher) {
        val theme = FakeTheme(ThemeMode.SYSTEM); val vm = model(theme); runCurrent()
        theme.mode.value = ThemeMode.DARK; runCurrent()
        assertEquals(ThemeMode.DARK, vm.state.value.selected)
        assertEquals(emptyList(), theme.chosen)
    }
}
