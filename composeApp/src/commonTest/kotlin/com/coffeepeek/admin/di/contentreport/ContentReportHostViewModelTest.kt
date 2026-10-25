package com.coffeepeek.admin.di.contentreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ContentReportHostViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val sessions = ReportTestSessions()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun host() = ContentReportHostViewModel(sessions).also { store.put("host", it) }
    private class Child : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    @Test fun sameUserRenewalKeepsTheSameChildAndDraftOwner() = runTest(dispatcher) {
        val host = host()
        val before = host.state.value
        sessions.current.value = Session("renewed", userId = "owner")
        runCurrent()
        assertSame(before, host.state.value)
        assertTrue(before.session.isCurrent())
    }

    @Test fun accountReplacementClearsTheOldFormStoreAndNeverRevivesIt() = runTest(dispatcher) {
        val host = host()
        val old = host.state.value
        val child = Child().also { old.session.viewModelStore.put("form", it) }
        sessions.current.value = Session("other-token", userId = "other")
        runCurrent()
        assertTrue(child.cleared)
        assertFalse(old.session.isCurrent())
        assertTrue(host.state.value.generation > old.generation)
        sessions.current.value = Session("token-a", userId = "owner")
        runCurrent()
        assertFalse(old.session.isCurrent())
    }

    @Test fun observationFailureHidesAccessAndRetryRecreatesAValidOwner() = runTest(dispatcher) {
        sessions.source = flow { error("private observation error") }
        val host = host()
        val failed = host.state.value
        assertTrue(failed.unavailable)
        sessions.source = sessions.current
        host.retry()
        runCurrent()
        assertFalse(host.state.value.unavailable)
        assertTrue(host.state.value.session.isLoggedIn)
        assertTrue(host.state.value.session.isCurrent())
        assertFalse(failed.session.isCurrent())
    }

    @Test fun finalDisposalClearsChildrenAndRevokesTheCapability() = runTest(dispatcher) {
        val host = host()
        val session = host.state.value.session
        val child = Child().also { session.viewModelStore.put("form", it) }
        store.clear()
        assertTrue(child.cleared)
        assertFalse(session.isCurrent())
    }

    @Test fun guestBecomingAuthenticatedGetsANewEmptyFormOwner() = runTest(dispatcher) {
        sessions.current.value = null
        val host = host()
        val guest = host.state.value.session
        assertFalse(guest.isLoggedIn)
        sessions.current.value = Session("token-a", userId = "owner")
        runCurrent()
        assertNotSame(guest, host.state.value.session)
        assertTrue(host.state.value.session.isLoggedIn)
        assertFalse(guest.isCurrent())
    }
}
