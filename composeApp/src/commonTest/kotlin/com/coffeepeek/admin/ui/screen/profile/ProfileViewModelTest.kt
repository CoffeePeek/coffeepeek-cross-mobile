package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.AppleLoginChallenge
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
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.ShopCatalogs
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopSubmission
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Parity tests for the legacy own-profile/settings ViewModel (profile migration step 1). Most tests use real
 * dispatchers; race regressions use controlled scopes. `GoogleAuth.signOut` is not observable from here.
 */
class ProfileViewModelTest {
    private val anna = UserProfile("Anna Lee", "anna@example.com", "About", "https://img/a.jpg", 0, 3, 2,
        PublicAddress("anna-lee", "/users/anna-lee", 1, false), 1, 4)
    private val boris = UserProfile("Boris", "boris@example.com", null, null, 0, 0, 0)

    @AfterTest fun tearDown() = ErrorHandler.clearError()

    private fun session(userId: String) = Session("token-$userId", null, userId)

    @Test fun signedInWithACachedAccountShowsItImmediatelyWithoutARequest() = runTest {
        val h = Harness(session("a"), cached = anna)
        val vm = h.viewModel()
        await { vm.uiState.value.displayName == "Anna Lee" }
        val s = vm.uiState.value
        assertTrue(s.isLoggedIn)
        assertEquals("AL", s.initials)
        assertEquals(3, s.checkInCount); assertEquals(2, s.addedShopsCount)
        assertEquals(1, s.addedRoastersCount); assertEquals(4, s.followersCount)
        assertEquals("anna-lee", s.publicSlug)
        assertFalse(s.isLoading)
        assertEquals(0, h.users.refreshCalls)
    }

    @Test fun withoutACacheItRefreshesAndAFailureWithoutContentShowsTheFixedError() = runTest {
        val h = Harness(session("a"), cached = null, refresh = Result.failure(RuntimeException("net")))
        val vm = h.viewModel()
        await { vm.uiState.value.error != null }
        assertEquals("Не удалось загрузить профиль", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun aLateEmptyCacheEmissionKeepsTheRefreshFailure() = runTest {
        val dispatcher = QueuedDispatcher()
        val scope = backgroundScope + dispatcher
        val h = Harness(session("a"), cached = null, refresh = Result.failure(RuntimeException("net")))
        val vm = h.viewModel(scope)
        try {
            dispatcher.runAt(1) // Observe the session before the cache collector starts.
            dispatcher.runAt(2) // Finish the refresh before the initial empty cache emission.
            assertEquals("Не удалось загрузить профиль", vm.uiState.value.error)
            dispatcher.runAt(0)
            assertEquals("Не удалось загрузить профиль", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.isLoggedIn)
            h.sessions.set(null)
            dispatcher.runAll()
            assertEquals(ProfileUiState(isLoading = false), vm.uiState.value)
        } finally {
            scope.cancel()
            dispatcher.runAll()
        }
    }

    @Test fun aFailedRefreshWithContentKeepsTheContentAndOnlySetsRefreshError() = runTest {
        val h = Harness(session("a"), cached = anna, refresh = Result.failure(RuntimeException("net")))
        val vm = h.viewModel()
        await { vm.uiState.value.displayName == "Anna Lee" }
        vm.refreshProfile()
        await { vm.uiState.value.refreshError != null }
        assertEquals("Не удалось загрузить профиль", vm.uiState.value.refreshError)
        assertNull(vm.uiState.value.error)
        assertEquals("Anna Lee", vm.uiState.value.displayName)
    }

    @Test fun endingTheSessionClearsEverything() = runTest {
        val h = Harness(session("a"), cached = anna)
        val vm = h.viewModel()
        await { vm.uiState.value.displayName == "Anna Lee" }
        h.sessions.set(null)
        await { !vm.uiState.value.isLoggedIn && vm.uiState.value.displayName.isEmpty() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun aLateFailureOfThePreviousAccountsRefreshIsIgnoredAfterASwitch() = runTest {
        val gate = CompletableDeferred<Result<UserProfile>>()
        val h = Harness(session("a"), cached = null, refresh = Result.success(boris), firstRefreshGate = gate)
        val vm = h.viewModel(backgroundScope)
        testScheduler.runCurrent()
        assertEquals(1, h.users.refreshCalls)
        h.sessions.set(session("b"))
        testScheduler.runCurrent()
        assertEquals(2, h.users.refreshCalls)
        h.users.profile.value = boris
        testScheduler.runCurrent()
        assertEquals("Boris", vm.uiState.value.displayName)
        gate.complete(Result.failure(RuntimeException("late")))
        testScheduler.runCurrent()
        assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.refreshError)
        assertEquals("Boris", vm.uiState.value.displayName)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test fun logoutResetsTheUiBeforeTheServerCallAndClearsDraftsAfterwards() = runTest {
        val order = MutableStateFlow<List<String>>(emptyList())
        lateinit var vm: ProfileViewModel
        val h = Harness(session("a"), cached = anna, onLogout = {
            order.update { it + ("logout|loggedIn=" + vm.uiState.value.isLoggedIn) }
        }, onClearDrafts = { order.update { it + "drafts" } })
        vm = h.viewModel()
        await { vm.uiState.value.displayName == "Anna Lee" }
        vm.logout()
        await { order.value.size == 2 }
        assertEquals(listOf("logout|loggedIn=false", "drafts"), order.value)
    }

    @Test fun aFailedLogoutShowsItsMessageOrTheLocalCleanupFallbackButStillClearsDrafts() = runTest {
        val cleared = MutableStateFlow(false)
        val h = Harness(session("a"), cached = anna, logout = Result.failure(RuntimeException("server said no")),
            onClearDrafts = { cleared.value = true })
        val vm = h.viewModel()
        await { vm.uiState.value.displayName == "Anna Lee" }
        vm.logout()
        await { cleared.value }
        assertEquals("server said no", ErrorHandler.errorMessage.value)
    }

    @Test fun theCityIsResolvedAtCreationEvenIfSettingsIsNeverOpenedAndUnknownIdsAreIgnored() = runTest {
        val h = Harness(session("a"), cached = anna, cities = listOf(City("minsk", "Минск"), City("grodno", "Гродно")))
        val vm = h.viewModel()
        await { vm.selectedCityId.value == "minsk" }
        vm.setCity("nowhere")
        delay(100)
        assertEquals("minsk", vm.selectedCityId.value)
        vm.setCity("grodno")
        assertEquals("grodno", vm.selectedCityId.value)
    }

    @Test fun aCatalogFailureLeavesTheCityListEmptyWithNoSelectionAndSetCityIgnoresEverything() = runTest {
        val h = Harness(session("a"), cached = anna, catalogs = Result.failure(IllegalStateException("offline")))
        val vm = h.viewModel()
        delay(300)
        assertTrue(vm.cities.value.isEmpty())
        assertNull(vm.selectedCityId.value)
        vm.setCity("minsk")
        assertNull(vm.selectedCityId.value)
    }

    @Test fun aSavedCitySurvivesTheNextStartAndAMissingOneFallsBack() = runTest {
        val cities = listOf(City("minsk", "Минск"), City("grodno", "Гродно"))
        val kept = Harness(session("a"), cached = anna, cities = cities, savedCity = "grodno").viewModel()
        await { kept.selectedCityId.value == "grodno" }
        assertEquals(listOf("minsk", "grodno"), kept.cities.value.map { it.id })
        val fallback = Harness(session("a"), cached = anna, cities = cities, savedCity = "removed").viewModel()
        await { fallback.selectedCityId.value == "minsk" }
    }

    @Test fun settingsReadTheLoggedInFlagFromTheSameStateThatClearsOnSessionEnd() = runTest {
        val h = Harness(session("a"), cached = anna)
        val vm = h.viewModel()
        await { vm.uiState.value.isLoggedIn }
        h.sessions.set(null)
        await { !vm.uiState.value.isLoggedIn }
    }

    private suspend fun await(condition: () -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition()) delay(10) } }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = mutableListOf<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.add(block) }
        fun runAt(index: Int) = tasks.removeAt(index).run()
        fun runAll() {
            while (tasks.isNotEmpty()) runAt(0)
        }
    }

    private class Sessions(initial: Session?) : SessionRepository {
        private val state = MutableStateFlow(initial)
        fun set(session: Session?) { state.value = session }
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) { state.value = session }
        override suspend fun saveSession(session: Session?) { state.value = session }
        override suspend fun warmCache() = Unit
        override fun observeSession(): Flow<Session?> = state
        override suspend fun isLoggedIn() = state.value != null
    }

    private class Users(cached: UserProfile?, private val refresh: Result<UserProfile>,
        private val firstRefreshGate: CompletableDeferred<Result<UserProfile>>?) : UserRepository {
        val profile = MutableStateFlow(cached)
        private val calls = MutableStateFlow(0)
        val refreshCalls: Int get() = calls.value
        override fun observeProfile(): StateFlow<UserProfile?> = profile
        override suspend fun refreshProfile(): Result<UserProfile> {
            val call = calls.getAndUpdate { it + 1 }
            if (call == 0 && firstRefreshGate != null) return firstRefreshGate.await()
            return refresh
        }
        override suspend fun getMe() = refresh
        override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = Result.success(null)
        override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("unused")
        override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("unused")
        override suspend fun updateUsername(username: String) = Result.success(Unit)
        override suspend fun updateAbout(about: String) = Result.success(Unit)
        override suspend fun updateAvatar(photo: PendingPhotoUpload) = Result.success(Unit)
    }

    private class Auth(private val result: Result<Unit>, private val onLogout: () -> Unit) : AuthRepository {
        override suspend fun login(email: String, password: String): Result<Session> = error("unused")
        override suspend fun googleLogin(idToken: String): Result<Session> = error("unused")
        override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> = error("unused")
        override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> = error("unused")
        override suspend fun register(userName: String, email: String, password: String): Result<Unit> = error("unused")
        override suspend fun isEmailTaken(email: String): Result<Boolean> = error("unused")
        override suspend fun logout(): Result<Unit> { onLogout(); return result }
    }

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

    private class Settings(private val onClearDrafts: () -> Unit, private val savedCity: String? = null) : SettingRepository {
        override suspend fun save(model: Setting) = Unit
        override suspend fun read(key: String): Setting? = if (key == "selected_city_id") savedCity?.let { Setting(key, it) } else null
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll(): List<Setting> { onClearDrafts(); return emptyList() }
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) = Unit
    }

    private object Notifications : NotificationFeature {
        override val state: StateFlow<NotificationInboxState> get() = error("unused")
        override fun refresh(): Unit = error("unused")
        override fun loadMore(): Unit = error("unused")
        override fun setPushEnabled(enabled: Boolean): Unit = error("unused")
        override fun markRead(id: String): Unit = error("unused")
        override fun markAllRead(): Unit = error("unused")
        override suspend fun destination(notificationId: String) = null
        override fun foreground(): Unit = error("unused")
        override fun onCheckInCreated(): Unit = error("unused")
        override fun onInboxOpened(): Unit = error("unused")
        override suspend fun beforeLogout() = Unit
    }

    private inner class Harness(
        initial: Session?, cached: UserProfile?,
        refresh: Result<UserProfile> = Result.failure(IllegalStateException("none")),
        firstRefreshGate: CompletableDeferred<Result<UserProfile>>? = null,
        private val logout: Result<Unit> = Result.success(Unit),
        private val onLogout: () -> Unit = {},
        private val onClearDrafts: () -> Unit = {},
        private val cities: List<City> = emptyList(),
        private val catalogs: Result<ShopCatalogs> = Result.success(ShopCatalogs(cities = cities)),
        private val savedCity: String? = null,
    ) {
        val sessions = Sessions(initial)
        val users = Users(cached, refresh, firstRefreshGate)
        fun viewModel(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) =
            ProfileViewModel(Auth(logout, onLogout), users, sessions, Shops(catalogs),
                CityPreference(Settings({}, savedCity)), ReviewDraftStore(Settings(onClearDrafts)), Notifications, scope)
    }
}
