package com.coffeepeek.admin.di.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
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
class AndroidShopBrowsingHostViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun sessionReplacementClearsFeatureStoreWithoutAnyUiSubscriber() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidShopBrowsingHostViewModel(sessions).also { store.put("host", it) }
        val first = host.session.value
        val child = ProbeViewModel().also { first.viewModelStore.put("feature", it) }
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertSame(first, host.session.value)
        assertFalse(child.cleared)
        sessions.applySession(null)
        runCurrent()
        assertTrue(child.cleared)
        assertFalse(first.lease.isCurrent())
        assertEquals(first.generation + 1, host.session.value.generation)
        assertTrue(host.session.value.lease.isCurrent())
        val guest = host.session.value
        sessions.applySession(Session("next", userId = "other"))
        runCurrent()
        assertFalse(guest.lease.isCurrent())
        assertEquals(first.generation + 2, host.session.value.generation)
    }

    @Test fun parentDisposalClearsChildrenAndRevokesCallbacks() = runTest(dispatcher) {
        val host = AndroidShopBrowsingHostViewModel(CheckInTestSessions()).also { store.put("host", it) }
        val session = host.session.value
        val child = ProbeViewModel().also { session.viewModelStore.put("feature", it) }
        store.clear()
        assertTrue(child.cleared)
        assertFalse(session.lease.isCurrent())
    }

    @Test fun authorVisibilityChangeClearsFeatureStoreEvenWhenRouteIsCovered() = runTest(dispatcher) {
        val snapshots = kotlinx.coroutines.flow.MutableStateFlow(com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot())
        val visibility = object : com.coffeepeek.feature.authorblocking.api.AuthorVisibility {
            override val visibility = snapshots
            override suspend fun refresh() = Result.success(Unit)
        }
        val host = AndroidShopBrowsingHostViewModel(CheckInTestSessions(), visibility).also { store.put("host", it) }
        runCurrent()
        val first = host.session.value
        val child = ProbeViewModel().also { first.viewModelStore.put("feature", it) }
        snapshots.value = snapshots.value.copy(revision = 1, blockedIds = setOf("author"))
        assertFalse(first.lease.isCurrent())
        runCurrent()
        assertTrue(child.cleared)
        assertTrue(host.session.value.lease.isCurrent())
        assertEquals(first.generation + 1, host.session.value.generation)
    }

    @Test fun observationFailureHidesAccountDataAndExplicitRetryCreatesAFreshStore() = runTest(dispatcher) {
        var fail = true
        val original = CheckInTestSessions()
        val sessions = object : SessionRepository by original {
            override fun observeSession() = if (fail) flow<Session?> { error("Session observation failed") }
                else original.observeSession()
        }
        val host = AndroidShopBrowsingHostViewModel(sessions).also { store.put("host", it) }
        runCurrent()
        assertTrue(host.session.value.observationFailed)
        val failed = host.session.value
        fail = false
        host.retry()
        runCurrent()
        assertFalse(host.session.value.observationFailed)
        assertTrue(host.session.value.lease.isCurrent())
        assertFalse(failed.lease.isCurrent())
        assertEquals(failed.generation + 1, host.session.value.generation)
    }

    private class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
