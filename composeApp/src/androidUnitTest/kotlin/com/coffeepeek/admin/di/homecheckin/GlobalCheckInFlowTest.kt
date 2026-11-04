package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlin.test.*

/** App-only navigation/account/analytics policy; no backend, production Application or form writes. */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalCheckInFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()
    private val services = mutableListOf<AnalyticsService>()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() {
        stores.forEach { it.clear() }
        // runTest has already cancelled each fake service actor; do not await a new command here.
        services.forEach { it.setForeground(false) }
        Dispatchers.resetMain()
    }

    private class Entry : HomeCheckInEditorEntry {
        @Composable override fun Content(preparationId: String?, sessionGeneration: Int, accessAllowed: Boolean,
            onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowedChange: (Boolean) -> Unit,
            modifier: Modifier, bean: HomeCheckInBean?) = error("Unit navigation tests must not render a form")
    }
    private class Fixture(val model: GlobalCheckInFlow, val store: ViewModelStore,
        val sdk: RecordingTransport, val service: AnalyticsService, val handoffs: MutableList<CheckInTracker?>)

    private suspend fun TestScope.fixture(sessions: SessionRepository = CheckInTestSessions(), accepted: Boolean = false,
        homeBean: HomeCheckInBean? = null): Fixture {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(if (accepted) AnalyticsConsent.Accepted else AnalyticsConsent.Declined), sdk)
            .also(services::add)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner")
        Analytics.install(service)
        val handoffs = mutableListOf<CheckInTracker?>()
        val model = GlobalCheckInFlow(sessions, homeBean) { lease, tracker ->
            assertTrue(lease.isCurrent() && lease.isLoggedIn)
            handoffs += tracker
            Entry()
        }
        val store = ViewModelStore().also(stores::add)
        store.put("global-create", model)
        return Fixture(model, store, sdk, service, handoffs)
    }
    private fun GlobalCheckInFlow.homeDestination() = assertIs<GlobalCheckInDestination.Home>(state.value)
    private fun GlobalCheckInFlow.closed() = assertIs<GlobalCheckInDestination.Closed>(state.value)

    @Test fun pickerIsLazyAndHomeFactoryRunsOnceOnlyAfterExplicitSelection() = runTest(dispatcher) {
        val f = fixture()
        assertSame(GlobalCheckInDestination.Picker, f.model.state.value)
        assertTrue(f.handoffs.isEmpty())
        f.model.home(); f.model.home(); f.model.shop()
        assertEquals(1, f.handoffs.size)
        assertNotNull(f.handoffs.single())
        assertIs<GlobalCheckInDestination.Home>(f.model.state.value)
    }

    @Test fun aShelfBrewSkipsThePickerAndClosesInsteadOfGoingBack() = runTest(dispatcher) {
        val f = fixture(homeBean = HomeCheckInBean("kenya", "Кения"))
        val home = f.model.homeDestination()
        assertEquals(1, f.handoffs.size)
        assertEquals(HomeCheckInBean("kenya", "Кения"), f.model.homeBean)
        f.model.shop(); f.model.shelfBag()
        assertSame(home, f.model.state.value)
        f.model.cancel(home)
        assertEquals(GlobalCheckInOutcome.Close, assertNotNull(f.model.claim(f.model.closed())).outcome)
    }

    @Test fun shelfBagLeavesThePickerWithoutAHomeEditorOrShopTracker() = runTest(dispatcher) {
        val f = fixture(accepted = true)
        f.model.shelfBag(); f.model.home(); f.model.shop()
        val completion = assertNotNull(f.model.claim(f.model.closed()))
        assertEquals(GlobalCheckInOutcome.ShelfBag, completion.outcome)
        assertNull(completion.shopTracker)
        assertTrue(f.handoffs.isEmpty())
        assertNull(f.model.claim(f.model.closed()))
    }

    @Test fun shopHandoffSkipsHomeAndCanBeDeliveredOnlyOnce() = runTest(dispatcher) {
        val f = fixture(accepted = true)
        f.model.shop(); f.model.shop(); f.model.home()
        val completion = assertNotNull(f.model.claim(f.model.closed()))
        assertEquals(GlobalCheckInOutcome.Shop, completion.outcome)
        assertNotNull(completion.shopTracker)
        assertTrue(f.handoffs.isEmpty())
        assertNull(f.model.claim(f.model.closed()))
        f.store.clear(); runCurrent()
        assertEquals(1, f.sdk.events.count { it.first == "check_in_kind_selected" })
        assertEquals(0, f.sdk.events.count { it.first == "check_in_cancelled" })
    }

    @Test fun pickerCancelIsTerminalAndDoesNotInitializeHome() = runTest(dispatcher) {
        val f = fixture(accepted = true)
        val picker = f.model.state.value
        f.model.cancel(picker); f.model.cancel(picker); f.model.home(); f.model.shop()
        assertEquals(GlobalCheckInOutcome.Close, f.model.claim(f.model.closed())?.outcome)
        f.store.clear(); runCurrent()
        assertTrue(f.handoffs.isEmpty())
        assertEquals(1, f.sdk.events.count { it.first == "check_in_cancelled" })
    }

    @Test fun homeAndParentShareOneTrackerWithoutDuplicatingStartAttemptOrCreated() = runTest(dispatcher) {
        val f = fixture(accepted = true)
        f.model.home()
        val tracker = assertNotNull(f.handoffs.single())
        val adapter = HomeCheckInTrackerAnalytics(f.model::canAccess, false, CheckInSource.Global, tracker)
        adapter.record(com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInEditorMetric.Opened)
        tracker.submit(); tracker.saved("Private", false)
        f.model.saved(f.model.homeDestination(), "created")
        assertEquals(GlobalCheckInOutcome.Saved, f.model.claim(f.model.closed())?.outcome)
        f.store.clear(); runCurrent()
        assertEquals(0, f.sdk.events.count { it.first == "check_in_start_attempted" }) // Caller owns the button attempt.
        assertEquals(1, f.sdk.events.count { it.first == "check_in_flow_started" })
        assertEquals(1, f.sdk.events.count { it.first == "check_in_saved" })
        assertEquals(1, f.sdk.events.count { it.first == "check_in_created" })
        assertEquals(0, f.sdk.events.count { it.first == "check_in_cancelled" })
        assertEquals(1, f.sdk.events.mapNotNull { it.second["flow_id"] }.toSet().size)
    }

    @Test fun busyFormCannotBeDismissedButItsConfirmedSaveIsTerminalExactlyOnce() = runTest(dispatcher) {
        val f = fixture()
        f.model.home()
        val home = f.model.homeDestination()
        f.model.setDismissAllowed(home, false)
        f.model.cancel(home); f.model.cancel(GlobalCheckInDestination.Picker); f.model.home(); f.model.shop()
        assertSame(home, f.model.state.value)
        f.model.saved(home, "saved"); f.model.saved(home, "saved")
        val closed = f.model.closed()
        assertEquals(GlobalCheckInOutcome.Saved, f.model.claim(closed)?.outcome)
        assertNull(f.model.claim(closed))
        assertEquals(1, f.handoffs.size)
    }

    @Test fun blankSaveIdCannotReportSuccessAndOldPickerCannotCloseAForm() = runTest(dispatcher) {
        val f = fixture()
        val picker = f.model.state.value
        f.model.home()
        val home = f.model.homeDestination()
        f.model.saved(home, " "); f.model.cancel(picker)
        assertSame(home, f.model.state.value)
    }

    @Test fun unlockedFormCancelAndLaterDisposalEmitOnlyOneCancellation() = runTest(dispatcher) {
        val f = fixture(accepted = true)
        f.model.home()
        val home = f.model.homeDestination()
        val tracker = assertNotNull(f.handoffs.single())
        f.model.cancel(home)
        tracker.cancel("close") // The form's own disposal can report the same terminal outcome.
        f.store.clear(); runCurrent()
        assertEquals(GlobalCheckInOutcome.Close, f.model.claim(f.model.closed())?.outcome)
        assertEquals(1, f.sdk.events.count { it.first == "check_in_cancelled" })
    }

    @Test fun guestAndExpiredCredentialsNeverOpenEditorOrStartAnalytics() = runTest(dispatcher) {
        for (session in listOf(null, Session("", userId = "owner"))) {
            val f = fixture(CheckInTestSessions(session), accepted = true)
            f.model.home(); f.model.shop(); runCurrent()
            assertTrue(f.handoffs.isEmpty())
            assertEquals(GlobalCheckInOutcome.Close, f.model.claim(f.model.closed())?.outcome)
            assertTrue(f.sdk.events.isEmpty())
        }
    }

    @Test fun tokenRenewalKeepsTheSameFormAndAllowsItsOutcome() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val f = fixture(sessions)
        f.model.home()
        val home = f.model.homeDestination()
        sessions.applySession(Session("renewed", userId = "owner")); runCurrent()
        assertSame(home, f.model.state.value)
        f.model.saved(home, "saved")
        assertEquals(GlobalCheckInOutcome.Saved, f.model.claim(f.model.closed())?.outcome)
    }

    @Test fun accountReplacementRejectsLateSaveUnlockAndPickerCallbacks() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val f = fixture(sessions)
        f.model.home()
        val previous = f.model.homeDestination()
        sessions.applySession(Session("other", userId = "other")); runCurrent()
        f.model.saved(previous, "private-id"); f.model.setDismissAllowed(previous, true)
        f.model.cancel(previous); f.model.home(); f.model.shop()
        assertEquals(GlobalCheckInOutcome.Close, f.model.claim(f.model.closed())?.outcome)
        assertEquals(1, f.handoffs.size)
        assertFalse(f.model.lease.isCurrent())
    }

    @Test fun replacementBeforeOutcomeDeliveryDoesNotRefreshOrNavigateForAnotherAccount() = runTest(dispatcher) {
        for (shop in listOf(false, true)) {
            val sessions = CheckInTestSessions()
            val f = fixture(sessions)
            if (shop) f.model.shop() else {
                f.model.home(); f.model.saved(f.model.homeDestination(), "saved")
            }
            val oldCompletion = f.model.closed()
            sessions.applySession(null) // No collector tick needed for the independent lease guard.
            assertEquals(GlobalCheckInOutcome.Close, f.model.claim(oldCompletion)?.outcome)
            runCurrent()
            assertNull(f.model.claim(f.model.closed()))
        }
    }

    @Test fun sessionObserverFailureClosesAndPermanentlyRevokesThePrivateFlow() = runTest(dispatcher) {
        val sessions = object : SessionRepository by CheckInTestSessions() {
            override fun observeSession() = flow<Session?> { throw IllegalStateException("Private transport details") }
        }
        val f = fixture(sessions)
        f.model.home(); runCurrent()
        assertFalse(f.model.canAccess())
        assertTrue(f.handoffs.isEmpty())
        assertEquals(GlobalCheckInOutcome.Close, f.model.claim(f.model.closed())?.outcome)
    }

    @Test fun disposingAnOpenFormRevokesItsLeaseAndCancelsTheOneFlow() = runTest(dispatcher) {
        val f = fixture(accepted = true)
        f.model.home()
        val previous = f.model.homeDestination()
        f.store.clear()
        f.model.saved(previous, "late"); f.model.home(); runCurrent()
        assertFalse(f.model.canAccess())
        assertEquals(1, f.handoffs.size)
        assertSame(previous, f.model.state.value)
        assertEquals(1, f.sdk.events.count { it.first == "check_in_cancelled" })
    }

    @Test fun consentDeclinedAtOpeningDoesNotReplayAfterAcceptance() = runTest(dispatcher) {
        val f = fixture()
        f.service.setConsent(AnalyticsConsent.Accepted)
        f.model.home()
        assertNotNull(f.handoffs.single()).saved("Private", false)
        f.model.saved(f.model.homeDestination(), "saved"); runCurrent()
        assertTrue(f.sdk.events.isEmpty())
    }
}
