package com.coffeepeek.feature.profile.impl.ui

import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileError
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileSignOutPort
import com.coffeepeek.feature.profile.domain.ProfileAddress
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileAction
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Port of the account half of the legacy `ProfileViewModelTest`. */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val anna = OwnProfile("Anna Lee", "anna@example.com", "About", "https://img/a.jpg", 3, 2, 1, 4, ProfileAddress("anna-lee", "/users/anna-lee", 1, false))
    private val boris = OwnProfile("Boris", "boris@example.com", null, null, 0, 0, 0, 0, null)

    private fun model(accounts: Accounts, signOut: ProfileSignOutPort = ProfileSignOutPort.None) =
        OwnProfileViewModel(accounts, signOut).also { store.put("own-${nextKey++}", it) }

    @Test fun aCachedAccountIsShownWithoutARequest() = runTest(dispatcher) {
        val accounts = Accounts("a", anna)
        val vm = model(accounts); runCurrent()
        val state = vm.state.value
        assertTrue(state.isLoggedIn); assertFalse(state.isLoading)
        assertEquals("Anna Lee", state.profile?.userName)
        assertEquals("anna-lee", state.publicSlug)
        assertEquals(0, accounts.loads)
    }

    @Test fun withoutACacheItRefreshesAndAFailureWithoutContentShowsTheFixedError() = runTest(dispatcher) {
        val vm = model(Accounts("a", null).apply { load = { Result.failure(RuntimeException("net")) } }); runCurrent()
        assertEquals(OwnProfileError.LoadFailed, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun aFailedRefreshWithContentKeepsItAndOnlySetsRefreshError() = runTest(dispatcher) {
        val accounts = Accounts("a", anna).apply { load = { Result.failure(RuntimeException("net")) } }
        val vm = model(accounts); runCurrent()
        vm.onAction(OwnProfileAction.Refresh); runCurrent()
        assertEquals(OwnProfileError.LoadFailed, vm.state.value.refreshError)
        assertNull(vm.state.value.error)
        assertEquals("Anna Lee", vm.state.value.profile?.userName)
    }

    @Test fun endingTheSessionClearsEverything() = runTest(dispatcher) {
        val accounts = Accounts("a", anna)
        val vm = model(accounts); runCurrent()
        accounts.signIn(null); runCurrent()
        assertFalse(vm.state.value.isLoggedIn)
        assertNull(vm.state.value.profile)
    }

    @Test fun aLateFailureOfThePreviousAccountsRefreshIsIgnoredAfterASwitch() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<OwnProfile>>()
        val accounts = Accounts("a", null).apply { load = { gate.await() } }
        val vm = model(accounts); runCurrent()
        accounts.signIn("b"); accounts.publish(boris); runCurrent()
        gate.complete(Result.failure(RuntimeException("late"))); runCurrent()
        assertNull(vm.state.value.error)
        assertNull(vm.state.value.refreshError)
        assertEquals("Boris", vm.state.value.profile?.userName)
    }

    @Test fun logoutResetsTheScreenBeforeTheAppEndsTheAccount() = runTest(dispatcher) {
        lateinit var vm: OwnProfileViewModel
        val seen = mutableListOf<Boolean>()
        val signOut = object : ProfileSignOutPort { override suspend fun signOut() { seen += vm.state.value.isLoggedIn } }
        vm = model(Accounts("a", anna), signOut); runCurrent()
        vm.onAction(OwnProfileAction.Logout); runCurrent()
        assertEquals(listOf(false), seen)
        assertNull(vm.state.value.profile)
    }

    private class Accounts(user: String?, own: OwnProfile?) : ProfileAccountPort {
        private val account = MutableStateFlow(user)
        private val cache = MutableStateFlow(own)
        var loads = 0
        var load: suspend () -> Result<OwnProfile> = { Result.failure(IllegalStateException("none")) }
        fun signIn(user: String?) { account.value = user }
        fun publish(value: OwnProfile?) { cache.value = value }
        override fun activeAccount() = account.value
        override fun observeActiveAccount(): Flow<String?> = account
        override fun cachedOwnProfile() = cache.value
        override fun observeOwnProfile(): Flow<OwnProfile?> = cache
        override suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile> {
            if (!forceRefresh) cache.value?.let { return Result.success(it) }
            loads++
            return load().onSuccess { cache.value = it }
        }
    }
}
