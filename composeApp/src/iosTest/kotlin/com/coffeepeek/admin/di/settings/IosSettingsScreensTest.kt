package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.SettingsExternalActions
import com.coffeepeek.feature.settings.api.UsageConsentPort
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.domain.UsageConsentState
import com.coffeepeek.feature.settings.impl.NativeCitySettings
import com.coffeepeek.feature.settings.impl.NativeLanguageSettings
import com.coffeepeek.feature.settings.impl.NativeUsagePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosSettingsScreensTest {
    @Test
    fun cityRetryUsesTheLoadedCatalogAndRejectsUnknownIds() = settingsTest {
        val port = CityFixture()
        val screen = IosCitySettings(NativeCitySettings(port), LanguageFixture(), {})
        try {
            runCurrent()
            assertTrue(screen.currentState().failed)
            screen.retry()
            runCurrent()
            assertEquals(listOf("minsk"), screen.currentState().cities.map { it.id })
            screen.select("unknown")
            runCurrent()
            assertEquals(null, screen.currentState().selectedId)
            screen.select("minsk")
            runCurrent()
            assertEquals("minsk", screen.currentState().selectedId)
        } finally {
            screen.close()
        }
    }

    @Test
    fun languageObserverPublishesOnlySuccessfullySavedChoices() = settingsTest {
        val language = LanguageFixture()
        val received = mutableListOf<String>()
        val screen = IosLanguageSettings(NativeLanguageSettings(language), language, {})
        val observation = screen.observeLanguage { received.add(it) }
        try {
            runCurrent()
            screen.select("be")
            runCurrent()
            assertTrue(screen.currentState().saveFailed)
            assertEquals(listOf("ru"), received)
            screen.retry()
            runCurrent()
            assertFalse(screen.currentState().saveFailed)
            assertEquals(listOf("ru", "be"), received)
        } finally {
            observation.cancel()
            screen.close()
        }
    }

    @Test
    fun guestConsentDoesNotCallWriter() = settingsTest {
        val consent = ConsentFixture()
        val screen = IosUsagePolicy(NativeUsagePolicy(consent), LanguageFixture(), {}, ExternalFixture)
        try {
            runCurrent()
            screen.setConsent(true)
            runCurrent()
            assertEquals(emptyList(), consent.changes)
            consent.state.value = UsageConsentState(available = true, accepted = false)
            runCurrent()
            screen.setConsent(true)
            runCurrent()
            assertEquals(listOf(true), consent.changes)
        } finally {
            screen.close()
        }
    }

    private fun settingsTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class CityFixture : CityPort {
        private val selected = MutableStateFlow<String?>(null)
        private var loads = 0

        override fun observeSelectedCityId() = selected

        override suspend fun loadCities(): Result<List<SettingsCity>> =
            if (++loads == 1) Result.failure(IllegalStateException("offline"))
            else Result.success(listOf(SettingsCity("minsk", "Минск")))

        override suspend fun select(cityId: String) {
            selected.value = cityId
        }
    }

    private class LanguageFixture : LanguagePort {
        private val language = MutableStateFlow(AppLanguage.RUSSIAN)
        private var saves = 0

        override fun current() = language.value

        override fun observe() = language

        override suspend fun set(language: AppLanguage): Result<Unit> {
            if (++saves == 1) return Result.failure(IllegalStateException("write failed"))
            this.language.value = language
            return Result.success(Unit)
        }
    }

    private class ConsentFixture : UsageConsentPort {
        val state = MutableStateFlow(UsageConsentState.Unavailable)
        val changes = mutableListOf<Boolean>()

        override fun observe() = state

        override suspend fun set(accepted: Boolean) {
            changes.add(accepted)
        }
    }

    private object ExternalFixture : SettingsExternalActions {
        override val canRateApp = false
        override fun openTerms() = Unit
        override fun openPrivacy() = Unit
        override fun openSupport() = Unit
        override fun shareApp() = Unit
        override fun rateApp() = Unit
    }
}
