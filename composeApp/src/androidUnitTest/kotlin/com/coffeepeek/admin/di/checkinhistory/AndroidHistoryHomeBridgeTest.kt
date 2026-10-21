package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModel
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/** Navigation/lifetime regressions. Home's own MVI suites exercise actual drafts and writes. */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidHistoryHomeBridgeTest {
    private val dispatcher = StandardTestDispatcher()
    private val owned = mutableListOf<HistoryHostSession>()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { owned.forEach { it.clear() }; Dispatchers.resetMain() }

    private class Source : HomePreparationRepository {
        var calls = 0
        private fun <T> unexpected(): Result<T> { calls++; error("Opening a destination must not start repository work") }
        override suspend fun detail(id: String): Result<HomePreparation> = unexpected()
        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> = unexpected()
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> = unexpected()
        override suspend fun delete(id: String): Result<Unit> = unexpected()
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = unexpected()
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = unexpected()
    }

    private class ClearedModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    private fun bridge(sessions: CheckInTestSessions = CheckInTestSessions(), source: Source = Source(),
        onFactory: (CheckInTracker?) -> Unit = {}): Pair<HistoryHostSession, AndroidHistoryHomeBridge> {
        val session = HistoryHostSession(0, AndroidCheckInHistorySession(sessions)).also(owned::add)
        val entries = historyHomeEditorFactory(source, object : CoffeePicker {
            override suspend fun search(query: String) = Result.success(emptyList<CoffeePickerItem>())
        }, BrewMethodCatalog { Result.success(emptyList()) })
        val bridge = AndroidHistoryHomeBridge(session, HistoryHomeEditorFactory { lease, tracker ->
            onFactory(tracker); entries.create(lease, tracker)
        })
        session.viewModelStore.put("home", bridge)
        return session to bridge
    }

    @Test fun entryAssemblyIsLazyAndCreateDismissDoesNotReadOrWriteHistory() = runTest(dispatcher) {
        val source = Source()
        val (_, bridge) = bridge(source = source)
        assertNull(bridge.destination.value)
        bridge.create()
        val editor = assertNotNull(bridge.destination.value)
        assertTrue(editor.editing); assertNull(editor.id)
        bridge.dismiss(editor)
        assertNull(bridge.destination.value)
        assertEquals(0, source.calls)
    }

    @Test fun guestAndRevokedAccountCannotOpenOrCreate() = runTest(dispatcher) {
        val (_, guest) = bridge(CheckInTestSessions(null))
        guest.create(); guest.open("entry", true)
        assertNull(guest.destination.value)
        val (session, active) = bridge()
        session.lease.revoke()
        active.create(); active.open("entry")
        assertNull(active.destination.value)
    }

    @Test fun editCancellationReturnsToFreshDetailsThenDismissClosesThem() = runTest(dispatcher) {
        val (_, bridge) = bridge()
        bridge.open("owned", editOnLoad = true)
        val editor = assertNotNull(bridge.destination.value)
        assertTrue(editor.editing); assertEquals("owned", editor.id)
        bridge.dismiss(editor)
        val detail = assertNotNull(bridge.destination.value)
        assertFalse(detail.editing); assertEquals("owned", detail.id)
        assertNotSame(editor.viewModelStore, detail.viewModelStore)
        bridge.dismiss(detail)
        assertNull(bridge.destination.value)
    }

    @Test fun closingAndReopeningSameIdClearsTheOldStoreAndNeverReusesDetails() = runTest(dispatcher) {
        val (_, bridge) = bridge()
        bridge.open("owned")
        val previous = assertNotNull(bridge.destination.value)
        val model = ClearedModel()
        previous.viewModelStore.put("fixture", model)
        bridge.dismiss(previous); bridge.open("owned")
        assertTrue(model.cleared)
        assertNotSame(previous, bridge.destination.value)
        assertNotSame(previous.entries.detail, bridge.destination.value?.entries?.detail)
    }

    @Test fun detailEditAndReturnClearEachPreviousOwner() = runTest(dispatcher) {
        val (_, bridge) = bridge()
        bridge.open("owned")
        val detail = assertNotNull(bridge.destination.value)
        val model = ClearedModel().also { detail.viewModelStore.put("fixture", it) }
        bridge.edit(detail, "someone-else")
        assertSame(detail, bridge.destination.value)
        bridge.edit(detail, "owned")
        assertTrue(model.cleared)
        val editor = assertNotNull(bridge.destination.value)
        assertTrue(editor.editing)
        bridge.dismiss(editor)
        assertFalse(assertNotNull(bridge.destination.value).editing)
    }

    @Test fun busyDestinationRejectsCloseReplaceEditAndPhotosButAcceptsItsSave() = runTest(dispatcher) {
        val (session, bridge) = bridge()
        bridge.open("owned", true)
        val editor = assertNotNull(bridge.destination.value)
        bridge.setDismissAllowed(editor, false)
        bridge.dismiss(editor); bridge.create(); bridge.open("other"); bridge.edit(editor, "owned")
        bridge.openPhoto(editor, listOf("private"), 0) { _, _ -> error("Locked photo callback") }
        assertSame(editor, bridge.destination.value)
        bridge.saved(editor, "owned", false)
        assertEquals(1, session.contentGeneration.value)
        assertFalse(assertNotNull(bridge.destination.value).editing)
    }

    @Test fun createUpdateDeleteInvalidateExactlyOnceAndVisibilityNoticeReachesFreshDetails() = runTest(dispatcher) {
        val (session, bridge) = bridge()
        bridge.create()
        val create = assertNotNull(bridge.destination.value)
        bridge.saved(create, "created", true); bridge.saved(create, "created", true)
        val detail = assertNotNull(bridge.destination.value)
        assertTrue(detail.visibilityUnconfirmed)
        assertEquals(1, session.contentGeneration.value)
        bridge.edit(detail, "created")
        val edit = assertNotNull(bridge.destination.value)
        bridge.saved(edit, "created", false)
        val updated = assertNotNull(bridge.destination.value)
        assertFalse(updated.visibilityUnconfirmed)
        assertEquals(2, session.contentGeneration.value)
        bridge.setDismissAllowed(updated, false)
        bridge.deleted(updated, "created"); bridge.deleted(updated, "created")
        assertNull(bridge.destination.value)
        assertEquals(3, session.contentGeneration.value)
    }

    @Test fun invalidOrWrongOutcomeCannotInvalidateTheTimelineOrCloseAForm() = runTest(dispatcher) {
        val (session, bridge) = bridge()
        bridge.open("owned", true)
        val edit = assertNotNull(bridge.destination.value)
        bridge.saved(edit, "", false); bridge.saved(edit, "other", false); bridge.deleted(edit, "owned")
        assertSame(edit, bridge.destination.value)
        bridge.dismiss(edit)
        val detail = assertNotNull(bridge.destination.value)
        bridge.saved(detail, "owned", false); bridge.deleted(detail, "other")
        assertSame(detail, bridge.destination.value)
        assertEquals(0, session.contentGeneration.value)
    }

    @Test fun staleCallbacksCannotCloseUnlockOrMutateReplacementInTheSameAccount() = runTest(dispatcher) {
        val (session, bridge) = bridge()
        bridge.create()
        val previous = assertNotNull(bridge.destination.value)
        bridge.dismiss(previous); bridge.open("new", true)
        val next = assertNotNull(bridge.destination.value)
        bridge.setDismissAllowed(next, false)
        bridge.setDismissAllowed(previous, true); bridge.dismiss(previous)
        bridge.saved(previous, "old", false); bridge.deleted(previous, "old")
        bridge.edit(previous, "old")
        assertSame(next, bridge.destination.value)
        assertFalse(next.dismissAllowed.value)
        assertEquals(0, session.contentGeneration.value)
    }

    @Test fun replacedAccountRejectsLateSaveDeletePhotoAndDismissAndClearErasesStores() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val (session, bridge) = bridge(sessions)
        bridge.open("owned")
        val previous = assertNotNull(bridge.destination.value)
        val model = ClearedModel().also { previous.viewModelStore.put("fixture", it) }
        sessions.applySession(Session("other", userId = "other"))
        bridge.deleted(previous, "owned"); bridge.saved(previous, "owned", true)
        bridge.openPhoto(previous, listOf("private"), 0) { _, _ -> error("Stale photo") }
        bridge.dismiss(previous); bridge.create()
        assertEquals(0, session.contentGeneration.value)
        session.clear()
        assertTrue(model.cleared)
        assertNull(bridge.destination.value)
    }

    @Test fun sameAccountTokenRenewalPreservesDestinationAndItsResult() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val (session, bridge) = bridge(sessions)
        bridge.create()
        val current = assertNotNull(bridge.destination.value)
        sessions.applySession(Session("renewed", userId = "owner"))
        bridge.saved(current, "created", false)
        assertEquals("created", bridge.destination.value?.id)
        assertEquals(1, session.contentGeneration.value)
    }

    @Test fun photoNavigationIsRestrictedToTheCurrentUnlockedDetail() = runTest(dispatcher) {
        val (_, bridge) = bridge()
        bridge.open("owned")
        val detail = assertNotNull(bridge.destination.value)
        var received: Pair<List<String>, Int>? = null
        bridge.openPhoto(detail, listOf("photo"), 0) { urls, index -> received = urls to index }
        assertEquals(listOf("photo") to 0, received)
        var explicit: Triple<List<String>, List<String>, Int>? = null
        bridge.openPhoto(detail, listOf("detail", ""), listOf("original-a", "original-b"), 1) { urls, originals, index ->
            explicit = Triple(urls, originals, index)
        }
        assertEquals(Triple(listOf("detail", ""), listOf("original-a", "original-b"), 1), explicit)
        bridge.openPhoto(detail, listOf("detail"), emptyList(), 0) { _, _, _ -> error("Misaligned album") }
        bridge.setDismissAllowed(detail, false)
        bridge.openPhoto(detail, listOf("wrong"), 0) { _, _ -> error("Busy details") }
    }

    @Test fun blankRouteCannotReplaceAnExistingDestination() = runTest(dispatcher) {
        val (_, bridge) = bridge()
        bridge.create()
        val current = assertNotNull(bridge.destination.value)
        bridge.open(" ")
        assertSame(current, bridge.destination.value)
    }
}
