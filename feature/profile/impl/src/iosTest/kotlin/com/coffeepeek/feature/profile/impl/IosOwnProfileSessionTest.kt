package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileSignOutPort
import com.coffeepeek.feature.profile.impl.ui.OwnProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
class IosOwnProfileSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun anOldNativeTabNeverReceivesTheReplacementSessionsProfile() = runTest(dispatcher) {
        val accounts = Accounts()
        val session = nativeSession(accounts)
        val names = mutableListOf<String?>()
        val observation = session.observe { names += it.identity?.userName }
        try {
            runCurrent()
            assertEquals("First", names.last())
            accounts.replace()
            assertFalse(session.isCurrent())
            assertNull(session.current().identity)
            runCurrent()
            assertEquals(listOf("First", null), names.distinct())
            assertFalse(session.current().active)
        } finally {
            observation.cancel()
            session.close()
        }
    }

    @Test fun closingTheNativeTabCancelsTheOwnerReadAndObservation() = runTest(dispatcher) {
        val accounts = Accounts()
        val session = nativeSession(accounts)
        val observation = session.observe {}
        runCurrent()
        assertTrue(accounts.readStarted)
        session.close()
        runCurrent()
        assertTrue(accounts.readCancelled)
        assertTrue(observation.isCancelled)
        assertNull(session.current().identity)
    }

    private fun nativeSession(accounts: Accounts, lifecycle: (String) -> Unit = {}): IosOwnProfileSession =
        IosOwnProfileSession(accounts, OwnProfileViewModel(accounts, ProfileSignOutPort.None), lifecycle) {
            error("Fixture has no public profile")
        }

    @Test fun returningToTheTabReobservesTheSameActiveProfile() = runTest(dispatcher) {
        val accounts = Accounts()
        val session = nativeSession(accounts)
        val first = session.observe {}
        runCurrent()
        first.cancel()
        runCurrent()
        assertTrue(session.isCurrent())
        assertFalse(accounts.readCancelled)
        val names = mutableListOf<String?>()
        val resumed = session.observe { names += it.identity?.userName }
        runCurrent()
        assertEquals(listOf("First"), names)
        resumed.cancel()
        session.close()
    }

    @Test fun rebuildingTheRootAfterReplacementCreatesAnActiveProfile() = runTest(dispatcher) {
        val accounts = Accounts()
        val old = nativeSession(accounts)
        runCurrent()
        accounts.replace()
        runCurrent()
        assertFalse(old.current().active)
        old.close()
        val replacement = nativeSession(accounts)
        runCurrent()
        assertTrue(replacement.current().active)
        assertEquals("Replacement", replacement.current().identity?.userName)
        replacement.close()
    }

    @Test fun restoredCredentialsMustCreateANewLeaseInsteadOfRevivingAnUninitializedOne() = runTest(dispatcher) {
        val accounts = Accounts(loaded = false)
        val reasons = mutableListOf<String>()
        val initial = nativeSession(accounts, reasons::add)
        runCurrent()
        assertTrue(reasons.contains("invalidated reason=owner_uninitialized"))
        accounts.restore()
        runCurrent()
        assertFalse(initial.current().active)
        val restored = nativeSession(accounts)
        runCurrent()
        assertTrue(restored.current().active)
        assertEquals("First", restored.current().identity?.userName)
        initial.close()
        restored.close()
    }

    @Test fun onlyOwnerCloseEndsTheLeaseAndReportsItOnce() = runTest(dispatcher) {
        val reasons = mutableListOf<String>()
        val session = nativeSession(Accounts(), reasons::add)
        val observation = session.observe {}
        runCurrent()
        observation.cancel()
        assertTrue(session.current().active)
        session.close()
        session.close()
        assertEquals(listOf("created owner_present=true scope_present=true", "invalidated reason=closed"), reasons)
    }

    private class Accounts(loaded: Boolean = true) : ProfileAccountPort {
        private val account = MutableStateFlow("same-account".takeIf { loaded })
        private val generation = MutableStateFlow("first".takeIf { loaded })
        private val profile = MutableStateFlow(owner("First").takeIf { loaded })
        var readStarted = false
        var readCancelled = false

        fun restore() {
            account.value = "same-account"
            generation.value = "first"
            profile.value = owner("First")
        }

        fun replace() {
            generation.value = "second"
            profile.value = owner("Replacement")
        }

        override fun activeAccount() = account.value
        override fun observeActiveAccount() = account
        override fun cacheScope() = generation.value
        override fun observeCacheScope() = generation
        override fun cachedOwnProfile() = profile.value
        override fun observeOwnProfile() = profile

        override suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile> {
            readStarted = true
            try {
                awaitCancellation()
            } finally {
                readCancelled = true
            }
        }

        private fun owner(name: String) = OwnProfile(name, "fixture@example.invalid", null, null, 1, 2, 3, 4, null)
    }
}
