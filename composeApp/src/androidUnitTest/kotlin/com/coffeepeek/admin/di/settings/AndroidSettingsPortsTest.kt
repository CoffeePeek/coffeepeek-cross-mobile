@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.settings

import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.analytics
import com.coffeepeek.admin.analytics.MemoryConsent
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateState
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.FakeSettingRepository
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.utils.COFFEEPEEK_SHARE_TEXT
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.CoffeeDrinkDefinition
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.domain.model.CreateShopInput
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.ShopCatalogs
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopSubmission
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.domain.UpdateCheckOutcome
import com.coffeepeek.feature.settings.domain.UpdateCheckState
import com.coffeepeek.feature.settings.domain.UsageConsentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.coffeepeek.admin.theme.ThemeMode as LegacyThemeMode

class AndroidSettingsPortsTest {
    private suspend fun await(condition: () -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition()) delay(10) } }

    // --- theme -------------------------------------------------------------------------------------------------------------

    @Test fun themeMapsBothWaysAndDelegatesToTheGlobalManager() = runTest {
        val port = AndroidThemePort()
        for (mode in ThemeMode.entries) {
            port.set(mode)
            assertEquals(mode, port.current())
            assertEquals(LegacyThemeMode.valueOf(mode.name), ThemeManager.themeMode.value)
            assertEquals(mode, port.observe().first())
        }
        port.set(ThemeMode.SYSTEM)
    }

    // --- city --------------------------------------------------------------------------------------------------------------

    private class Shops(private val catalogs: Result<ShopCatalogs>) : ShopRepository {
        override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("unused")
        override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = error("unused")
        override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("unused")
        override suspend fun getCatalogs() = catalogs
        override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("unused")
        override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("unused")
        override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("unused")
        override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int):
            Result<PagedResult<ShopSubmission>> = error("unused")
    }

    private val cities = listOf(City("minsk", "Минск"), City("grodno", "Гродно"))

    @Test fun loadingTheCatalogResolvesTheSavedCityAndReturnsTheCities() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "removed"))
        val preference = CityPreference(settings)
        val port = AndroidCityPort(Shops(Result.success(ShopCatalogs(cities = cities))), preference)
        assertNull(port.observeSelectedCityId().first())
        assertEquals(listOf(SettingsCity("minsk", "Минск"), SettingsCity("grodno", "Гродно")), port.loadCities().getOrThrow())
        assertEquals("minsk", port.observeSelectedCityId().first())
        assertEquals("minsk", settings.valueOf("selected_city_id"))
    }

    @Test fun aCatalogFailureChangesNothing() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "grodno"))
        val port = AndroidCityPort(Shops(Result.failure(IllegalStateException("offline"))), CityPreference(settings))
        assertTrue(port.loadCities().isFailure)
        assertNull(port.observeSelectedCityId().first())
        assertEquals(emptyList(), settings.saves)
    }

    @Test fun selectingPersistsAndIgnoresBlankIds() = runTest {
        val settings = FakeSettingRepository()
        val port = AndroidCityPort(Shops(Result.success(ShopCatalogs(cities = cities))), CityPreference(settings))
        port.loadCities()
        port.select("grodno"); port.select("")
        assertEquals("grodno", port.observeSelectedCityId().first())
        await { settings.valueOf("selected_city_id") == "grodno" }
    }

    // --- account -----------------------------------------------------------------------------------------------------------

    private class Sessions(initial: Session?, private val active: (Session?) -> Boolean = { it != null }) : SessionRepository {
        val state = MutableStateFlow(initial)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = active(session)
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) { state.value = session }
        override suspend fun saveSession(session: Session?) { state.value = session }
        override suspend fun warmCache() = Unit
        override fun observeSession(): Flow<Session?> = state
        override suspend fun isLoggedIn() = state.value != null
    }

    @Test fun signedInNeedsAnActiveSessionWithAUserId() = runTest {
        assertTrue(AndroidSettingsAccountPort(Sessions(Session("t", null, "user"))).isSignedIn())
        assertFalse(AndroidSettingsAccountPort(Sessions(Session("t", null, null))).isSignedIn())
        assertFalse(AndroidSettingsAccountPort(Sessions(null)).isSignedIn())
        assertFalse(AndroidSettingsAccountPort(Sessions(Session("t", null, "user")) { false }).isSignedIn())
    }

    @Test fun theSignedInFlowReplaysAndEmitsOnlyChanges() = runTest {
        val sessions = Sessions(Session("t", null, "a"))
        val port = AndroidSettingsAccountPort(sessions)
        val seen = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { port.observeSignedIn().toList(seen) }
        sessions.state.value = Session("t2", null, "b")      // account switch: still signed in
        sessions.state.value = null
        sessions.state.value = Session("t3", null, "c")
        job.cancel()
        assertEquals(listOf(true, false, true), seen)
    }

    // --- consent -----------------------------------------------------------------------------------------------------------

    @Test fun withoutAnAnalyticsServiceTheConsentRowIsUnavailableAndTogglingDoesNothing() = runTest {
        val port = AndroidUsageConsentPort { null }
        assertEquals(UsageConsentState.Unavailable, port.observe().first())
        port.set(true)
    }

    @Test fun consentIsAvailableOnlyForASignedInUserAndFollowsTheToggle() = runTest {
        val store = MemoryConsent()
        val service = analytics(store)
        val port = AndroidUsageConsentPort { service }
        service.setForeground(true); service.restoreConsent()
        assertEquals(UsageConsentState(available = false, accepted = false), port.observe().first())

        service.syncIdentity("user-a"); runCurrent()
        assertEquals(UsageConsentState(available = true, accepted = false), port.observe().first())
        port.set(true)
        assertEquals(UsageConsentState(available = true, accepted = true), port.observe().first())
        assertEquals(AnalyticsConsent.Accepted, store.consent)
        port.set(false)
        assertEquals(UsageConsentState(available = true, accepted = false), port.observe().first())
        assertEquals(AnalyticsConsent.Declined, store.consent)

        service.syncIdentity(null); runCurrent()
        assertFalse(port.observe().first().available)
    }

    // --- update ------------------------------------------------------------------------------------------------------------

    private class Updates(var update: AppUpdate?) : AppUpdateRepository {
        override suspend fun fetch() = update
        override suspend fun dismissedVersion(): Long? = null
        override suspend fun dismiss(version: Long) = Unit
    }

    @Test fun theUpdateRowMapsTheCheckStateAndACheckAnswersWithTheManualResult() = runTest {
        val repository = Updates(AppUpdate(200, 0, "https://example.com/app.apk"))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val state = AppUpdateState(repository, scope, currentVersion = 100)
        val port = AndroidUpdateCheckPort(state)
        assertEquals(UpdateCheckState(), port.observe().first())
        assertEquals(UpdateCheckOutcome.Available, port.check())
        assertEquals(UpdateCheckState(checking = false, updateAvailable = true), port.observe().first())

        repository.update = AppUpdate(100, 0, "https://example.com/app.apk")
        assertEquals(UpdateCheckOutcome.UpToDate, port.check())
        assertEquals(UpdateCheckState(), port.observe().first())
        repository.update = null
        assertEquals(UpdateCheckOutcome.Failed, port.check())
    }

    @Test fun aCheckThatStartsWhileAnAutomaticOneRunsWaitsForItAndThenAsksAgain() = runTest {
        val release = CompletableDeferred<Unit>()
        var fetches = 0
        val repository = object : AppUpdateRepository {
            override suspend fun fetch(): AppUpdate? { fetches++; if (fetches == 1) release.await(); return null }
            override suspend fun dismissedVersion(): Long? = null
            override suspend fun dismiss(version: Long) = Unit
        }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val state = AppUpdateState(repository, scope, currentVersion = 100)
        state.check()                       // the automatic check on resume, still waiting
        val port = AndroidUpdateCheckPort(state)
        val result = async { port.check() }
        runCurrent()
        assertFalse(result.isCompleted)
        release.complete(Unit)
        assertEquals(UpdateCheckOutcome.Failed, result.await())   // the second, manual fetch also returned no policy
        assertEquals(2, fetches)
    }

    // --- external actions --------------------------------------------------------------------------------------------------

    @Test fun externalActionsUseTheLegacyTargets() {
        val opened = mutableListOf<String>(); val shared = mutableListOf<String>()
        val actions = AndroidSettingsExternalActions(openUrl = opened::add, shareText = shared::add)
        actions.openSupport(); actions.openTerms(); actions.openPrivacy(); actions.shareApp()
        assertEquals(listOf("https://coffeepeek.by/support", LegalUrls.TERMS, LegalUrls.PRIVACY), opened)
        assertEquals(listOf(COFFEEPEEK_SHARE_TEXT), shared)
        assertEquals("https://coffeepeek.by/terms", LegalUrls.TERMS)
        assertEquals("https://coffeepeek.by/privacy", LegalUrls.PRIVACY)
    }
}
