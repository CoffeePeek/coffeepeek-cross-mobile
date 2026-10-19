package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.homecheckin.HomeReadTestRepository
import com.coffeepeek.admin.di.homecheckin.homeReadFixture
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.domain.model.Session
import com.coffeepeek.admin.analytics.analytics
import com.coffeepeek.admin.analytics.MemoryConsent
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.feature.homecheckin.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidHistoryHomeBridgeTest {
    private val dispatcher = StandardTestDispatcher()
    private val owned = mutableListOf<HistoryHostSession>()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { owned.forEach { it.clear() }; Dispatchers.resetMain() }

    private class Repository(val bulk: HomeReadTestRepository = HomeReadTestRepository()) : HomeCheckInRepository by bulk {
        var saves = 0
        var updates = 0
        var deletes = 0
        var detailReads = 0
        var failure: Throwable? = null
        var waitWrite: suspend () -> Unit = {}
        var waitDetail: suspend () -> Unit = {}
        val entry = homeReadFixture()
        override suspend fun detail(id: String): Result<HomeCheckIn> {
            detailReads++
            waitDetail()
            return Result.success(entry.copy(id = id))
        }
        override suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn> {
            saves++; waitWrite()
            return failure?.let { Result.failure(it) } ?: Result.success(entry.copy(text = input.text))
        }
        override suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn> {
            updates++; waitWrite()
            return failure?.let { Result.failure(it) } ?: Result.success(entry.copy(id = id, text = input.text))
        }
        override suspend fun delete(id: String): Result<Unit> {
            deletes++; waitWrite()
            return failure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
    }

    private fun bridge(repository: Repository = Repository(), sessions: CheckInTestSessions = CheckInTestSessions()):
        Pair<HistoryHostSession, AndroidHistoryHomeBridge> {
        val session = HistoryHostSession(0, AndroidCheckInHistorySession(sessions)).also(owned::add)
        val factory = historyHomeEditorFactory(repository, object : CoffeePicker {
            override suspend fun search(query: String) = Result.success(emptyList<CoffeePickerItem>())
        }, BrewMethodCatalog { Result.success(listOf(BrewMethodOption("hario-v60", "Hario V60"))) })
        val bridge = AndroidHistoryHomeBridge(session, factory)
        session.viewModelStore.put("home", bridge)
        return session to bridge
    }

    private suspend fun <T> awaitReal(block: suspend () -> T): T =
        withContext(Dispatchers.Default) { withTimeout(5_000) { block() } }

    @Test fun lazyEditorDoesNotIssueDuplicateTimelineOrCalendarRequests() = runTest(dispatcher) {
        val repository = Repository()
        val (_, bridge) = bridge(repository)
        assertNull(bridge.editor.value)
        bridge.create()
        val editor = assertNotNull(bridge.editor.value)
        awaitReal { editor.state.first { !it.brewMethodsLoading && it.brewMethods.isNotEmpty() } }
        assertTrue(editor.state.value.editing)
        assertEquals(5, editor.state.value.draft.tasteScore)
        assertTrue(repository.bulk.queries.isEmpty())
        assertTrue(repository.bulk.ranges.isEmpty())
        assertEquals(0, repository.saves)
        bridge.dismiss()
        assertNull(bridge.editor.value)
    }

    @Test fun suppliedCreationTrackerSurvivesIntoHomeFieldsAndCancelsOnUserDismissWithoutAWrite() = runTest(dispatcher) {
        val sdk = com.coffeepeek.admin.analytics.RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner")
        com.coffeepeek.admin.analytics.Analytics.install(service)
        try {
            val repository = Repository()
            val (_, bridge) = bridge(repository)
            val flow = HistoryCreationAnalytics { true }
            flow.opened()
            bridge.create(flow.home())
            val editor = assertNotNull(bridge.editor.value)
            editor.updateDraft { it.copy(text = "private fixture note") }
            bridge.dismiss(); runCurrent()
            assertNull(bridge.editor.value)
            assertEquals(0, repository.saves)
            assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
            assertEquals(1, sdk.events.count { it.first == "check_in_cancelled" })
            val field = sdk.events.single { it.first == "check_in_field_interacted" }.second
            assertEquals("global_create", field["source"])
            assertEquals("home", field["kind"])
            assertEquals("note", field["field"])
            assertTrue(sdk.events.none { "private fixture note" in it.second.values })
        } finally { service.setConsent(com.coffeepeek.admin.analytics.AnalyticsConsent.Declined) }
    }

    @Test fun guestAndRevokedAccountCannotOpenOrCreateAnEditor() = runTest(dispatcher) {
        val repository = Repository()
        val (guest, bridge) = bridge(repository, CheckInTestSessions(null))
        bridge.create(); bridge.open("entry", true)
        assertNull(bridge.editor.value)
        val (owner, active) = bridge(repository)
        owner.lease.revoke()
        active.create(); active.open("entry")
        assertNull(active.editor.value)
        assertEquals(0, repository.detailReads)
        assertEquals(0, repository.saves)
        assertEquals(0, guest.contentGeneration.value)
    }

    @Test fun editByIdRetainsRecipeAndDismissReturnsToDetailBeforeClosing() = runTest(dispatcher) {
        val repository = Repository()
        val (_, bridge) = bridge(repository)
        bridge.open("owned-id", editOnLoad = true)
        val editor = assertNotNull(bridge.editor.value)
        awaitReal { editor.state.first { it.editing } }
        assertEquals("owned-id", editor.state.value.selected?.id)
        assertEquals(repository.entry.doseGrams, editor.state.value.draft.doseGrams)
        assertEquals(repository.entry.preparedOn, editor.state.value.draft.preparedOn)
        bridge.dismiss()
        assertFalse(editor.state.value.editing)
        assertEquals("owned-id", editor.state.value.selected?.id)
        assertSame(editor, bridge.editor.value)
        bridge.dismiss()
        assertNull(bridge.editor.value)
    }

    @Test fun successfulCreateUpdateDeleteEachInvalidateOnceAndDeleteClosesTheOverlay() = runTest(dispatcher) {
        val repository = Repository()
        val (session, bridge) = bridge(repository)
        bridge.create()
        val editor = assertNotNull(bridge.editor.value)
        bridge.withEditor(editor) { it.save() }
        awaitReal { editor.state.first { !it.saving && it.selected != null } }
        assertEquals(1, session.contentGeneration.value)
        bridge.withEditor(editor) { it.edit() }
        bridge.withEditor(editor) { it.updateDraft { draft -> draft.copy(text = "Changed") } }
        bridge.withEditor(editor) { it.save() }
        awaitReal { editor.state.first { !it.saving && it.selected?.text == "Changed" } }
        assertEquals(2, session.contentGeneration.value)
        bridge.withEditor(editor) { it.delete() }
        awaitReal { editor.state.first { !it.saving && it.selected == null } }
        runCurrent()
        assertNull(bridge.editor.value)
        assertEquals(3, session.contentGeneration.value)
        assertEquals(1, repository.saves)
        assertEquals(1, repository.updates)
        assertEquals(1, repository.deletes)
        assertTrue(repository.bulk.queries.isEmpty())
        assertTrue(repository.bulk.ranges.isEmpty())
    }

    @Test fun failedWriteKeepsDraftAndDoesNotInvalidateOrCloseIt() = runTest(dispatcher) {
        val repository = Repository().apply { failure = IllegalStateException("private backend details") }
        val (session, bridge) = bridge(repository)
        bridge.create()
        val editor = assertNotNull(bridge.editor.value)
        editor.updateDraft { it.copy(text = "Keep my draft") }
        bridge.withEditor(editor) { it.save() }
        awaitReal { editor.state.first { !it.saving && it.formError != null } }
        assertEquals("Keep my draft", editor.state.value.draft.text)
        assertTrue(editor.state.value.editing)
        assertSame(editor, bridge.editor.value)
        assertEquals(0, session.contentGeneration.value)
        assertFalse(editor.state.value.formError.orEmpty().contains("private backend"))
    }

    @Test fun pendingWriteLocksDismissReplaceAndDuplicateSubmissionButRenewalKeepsIt() = runTest(dispatcher) {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val sessions = CheckInTestSessions()
        val repository = Repository().apply { waitWrite = { started.complete(Unit); gate.await() } }
        val (session, bridge) = bridge(repository, sessions)
        bridge.create()
        val editor = assertNotNull(bridge.editor.value)
        try {
            bridge.withEditor(editor) { it.save() }
            awaitReal { started.await() }
            bridge.dismiss(); bridge.create(); bridge.open("other")
            bridge.withEditor(editor) { it.save() }
            assertSame(editor, bridge.editor.value)
            assertEquals(1, repository.saves)
            assertEquals(0, repository.detailReads)
            sessions.applySession(Session("renewed", userId = "owner"))
            gate.complete(Unit)
            awaitReal { editor.state.first { !it.saving } }
            assertEquals(1, session.contentGeneration.value)
        } finally { gate.complete(Unit) }
    }

    @Test fun replacedAccountCancelsLateWriteAndCannotInvalidateOrResurrectOldEditor() = runTest(dispatcher) {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val sessions = CheckInTestSessions()
        val repository = Repository().apply {
            waitWrite = { withContext(NonCancellable) { started.complete(Unit); gate.await(); finished.complete(Unit) } }
        }
        val (session, bridge) = bridge(repository, sessions)
        bridge.create()
        val editor = assertNotNull(bridge.editor.value)
        try {
            bridge.withEditor(editor) { it.save() }
            awaitReal { started.await() }
            sessions.applySession(Session("other", userId = "other"))
            session.clear()
            gate.complete(Unit)
            awaitReal { finished.await() }
            runCurrent()
            assertNull(bridge.editor.value)
            assertEquals(0, session.contentGeneration.value)
            bridge.withEditor(editor) { it.save() }
            assertEquals(1, repository.saves)
        } finally { gate.complete(Unit) }
    }

    @Test fun dismissedDetailCannotReopenAfterNonCooperativeReplyOrStaleCallback() = runTest(dispatcher) {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply {
            waitDetail = { withContext(NonCancellable) { started.complete(Unit); gate.await() } }
        }
        val (_, bridge) = bridge(repository)
        bridge.open("old")
        val previous = assertNotNull(bridge.editor.value)
        try {
            awaitReal { started.await() }
            bridge.dismiss()
            bridge.create()
            val next = assertNotNull(bridge.editor.value)
            bridge.withEditor(previous) { it.updateDraft { draft -> draft.copy(text = "stale") } }
            gate.complete(Unit)
            assertSame(next, bridge.editor.value)
            assertNull(next.state.value.draft.text)
            assertNull(previous.state.value.selected)
        } finally { gate.complete(Unit) }
    }

    @Test fun failedDeleteKeepsDetailsAndRequiresExplicitRetry() = runTest(dispatcher) {
        val repository = Repository().apply { failure = IllegalStateException("delete failed") }
        val (session, bridge) = bridge(repository)
        bridge.open("owned")
        val editor = assertNotNull(bridge.editor.value)
        awaitReal { editor.state.first { it.selected != null } }
        bridge.withEditor(editor) { it.delete() }
        awaitReal { editor.state.first { !it.saving && it.error != null } }
        runCurrent()
        assertSame(editor, bridge.editor.value)
        assertNotNull(editor.state.value.selected)
        assertEquals(1, repository.deletes)
        assertEquals(0, session.contentGeneration.value)
    }
}
