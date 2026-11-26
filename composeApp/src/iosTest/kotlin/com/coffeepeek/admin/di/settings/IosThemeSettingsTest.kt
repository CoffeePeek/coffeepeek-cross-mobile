package com.coffeepeek.admin.di.settings

import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.domain.ThemeMode as SettingsThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class IosThemeSettingsTest {
    @Test
    fun selectionReadsAndWritesAllModesThroughTheSharedPort() {
        val port = FakeThemePort()
        val settings = IosThemeSettings(port)
        assertEquals(ThemeMode.DARK, settings.currentMode())
        for (mode in ThemeMode.entries) {
            settings.selectMode(mode)
            assertEquals(mode.toSettings(), port.mode.value)
            assertEquals(mode, settings.currentMode())
        }
    }

    @Test
    fun observationReplaysCurrentModeAndStopsWhenCancelled() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val port = FakeThemePort()
        val received = mutableListOf<ThemeMode>()
        val observation = IosThemeSettings(port).observeMode { received.add(it) }
        try {
            runCurrent()
            port.set(SettingsThemeMode.LIGHT)
            runCurrent()
            observation.cancel()
            port.set(SettingsThemeMode.SYSTEM)
            runCurrent()
            assertEquals(listOf(ThemeMode.DARK, ThemeMode.LIGHT), received)
        } finally {
            observation.cancel()
            Dispatchers.resetMain()
        }
    }

    private class FakeThemePort : ThemePort {
        val mode = MutableStateFlow(SettingsThemeMode.DARK)

        override fun current() = mode.value

        override fun observe() = mode

        override fun set(mode: SettingsThemeMode) {
            this.mode.value = mode
        }
    }
}
