package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidCheckInHistoryHostViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun hiddenRouteAccountReplacementClearsChildrenButTokenRenewalDoesNot() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val initial = host.session.value
        val child = Probe().also { initial.viewModelStore.put("history", it) }
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertSame(initial, host.session.value)
        assertFalse(child.cleared)
        sessions.applySession(null)
        runCurrent()
        assertTrue(child.cleared)
        assertFalse(initial.lease.isCurrent())
        assertFalse(host.session.value.lease.isLoggedIn)
        assertEquals(1, host.session.value.generation)
        val guest = host.session.value
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        assertFalse(guest.lease.isCurrent())
        assertTrue(host.session.value.lease.isLoggedIn)
        assertEquals(2, host.session.value.generation)
    }

    @Test fun ownerDisposalRevokesCallbacksAndClearsFeatureStore() = runTest(dispatcher) {
        val host = AndroidCheckInHistoryHostViewModel(CheckInTestSessions()).also { store.put("host", it) }
        val session = host.session.value
        val child = Probe().also { session.viewModelStore.put("history", it) }
        store.clear()
        assertTrue(child.cleared)
        assertFalse(session.lease.isCurrent())
    }

    @Test fun observationFailureHidesDataUntilExplicitRetryWithFreshStore() = runTest(dispatcher) {
        var fail = true
        val source = CheckInTestSessions()
        val sessions = object : SessionRepository by source {
            override fun observeSession() = if (fail) flow<Session?> { error("Store unavailable") } else source.observeSession()
        }
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val failed = host.session.value
        assertTrue(failed.observationFailed)
        fail = false
        host.retry()
        runCurrent()
        assertFalse(host.session.value.observationFailed)
        assertFalse(failed.lease.isCurrent())
        assertTrue(host.session.value.lease.isCurrent())
        assertEquals(failed.generation + 1, host.session.value.generation)
    }

    private class Probe : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
