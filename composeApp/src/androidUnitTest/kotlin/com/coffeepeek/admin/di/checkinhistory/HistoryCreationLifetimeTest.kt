package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.MemoryConsent
import com.coffeepeek.admin.analytics.RecordingTransport
import com.coffeepeek.admin.analytics.analytics
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.domain.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryCreationLifetimeTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val sessions = CheckInTestSessions()
    private val sdk = RecordingTransport()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun host() = ViewModelProvider.create(store, viewModelFactory {
        initializer { AndroidCheckInHistoryHostViewModel(sessions) }
    })["history", AndroidCheckInHistoryHostViewModel::class]

    private fun lifetimeTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("fixture-owner")
        Analytics.install(service)
        try { block() } finally {
            store.clear()
            service.setConsent(AnalyticsConsent.Declined)
        }
    }

    @Test fun homeSelectionAfterOwnerRebindingKeepsOneGlobalFlow() = lifetimeTest {
        val original = host()
        original.session.value.creationAnalytics.opened()
        val rebound = host()
        assertSame(original, rebound)
        assertSame(original.session.value.creationAnalytics, rebound.session.value.creationAnalytics)
        val tracker = assertNotNull(rebound.session.value.creationAnalytics.home())
        tracker.submit()
        tracker.saved("Private", false)
        runCurrent()
        assertSingleCompletedFlow("home")
    }

    @Test fun shopHandoffAfterOwnerRebindingSurvivesFinalHistoryDisposal() = lifetimeTest {
        host().session.value.creationAnalytics.opened()
        val tracker = assertNotNull(host().session.value.creationAnalytics.shop())
        store.clear()
        tracker.submit()
        tracker.saved("Public", false)
        runCurrent()
        assertSingleCompletedFlow("visit")
    }

    @Test fun finalOwnerDisposalCancelsAnOpenPickerExactlyOnce() = lifetimeTest {
        val current = host().session.value
        current.creationAnalytics.opened()
        store.clear()
        store.clear()
        current.creationAnalytics.opened()
        assertNull(current.creationAnalytics.home())
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
        assertEquals(1, sdk.events.count { it.first == "check_in_cancelled" })
    }

    @Test fun replacementAccountCannotContinueOrEmitTheOldPickerFlow() = lifetimeTest {
        val owner = host()
        val previous = owner.session.value
        previous.creationAnalytics.opened()
        runCurrent()
        val before = sdk.events.toList()
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        assertNotSame(previous, owner.session.value)
        assertNull(previous.creationAnalytics.shop())
        previous.creationAnalytics.cancelled()
        previous.creationAnalytics.opened()
        runCurrent()
        assertEquals(before, sdk.events)
    }

    private fun assertSingleCompletedFlow(kind: String) {
        assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
        assertEquals(1, sdk.events.count { it.first == "check_in_kind_selected" })
        assertEquals(0, sdk.events.count { it.first == "check_in_cancelled" })
        assertEquals(1, sdk.events.mapNotNull { it.second["flow_id"] }.toSet().size)
        val created = sdk.events.single { it.first == "check_in_created" }.second
        assertEquals("global_create", created["source"])
        assertEquals(kind, created["kind"])
    }
}
