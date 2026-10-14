package com.coffeepeek.feature.checkinhistory.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryAddress
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryMode
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarContext
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInHistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Calendar(
        override val today: String = "2026-10-14",
        override val zoneId: String = "UTC",
        private val dates: Map<String, String> = emptyMap(),
        private val bounds: HistoryPeriod? = null,
    ) : HistoryCalendarContext {
        override fun period(month: HistoryMonth): HistoryPeriod = bounds
            ?: HistoryPeriod("${month.date(1)}T00:00:00Z", "${month.plusMonths(1).date(1)}T00:00:00Z")
        override fun localDate(instant: String): String? = dates[instant]
            ?: instant.substringBefore('T').takeIf { HistoryMonth.fromDate(it) != null }
    }

    private class Clock : HistoryCalendarProvider {
        var context: HistoryCalendarContext = Calendar()
        var fails = false
        override fun snapshot(): HistoryCalendarContext {
            check(!fails) { "Private platform error must not enter rendering state" }
            return context
        }
    }

    private class Repository : CheckInHistoryRepository {
        val periods = mutableListOf<HistoryPeriod>()
        val pages = mutableListOf<Pair<Int, Int>>()
        var calendarResult: Result<List<PersonalCheckIn>> = Result.success(listOf(entry("calendar")))
        var pageResult = Result.success(page(1, "first"))
        var calendarGate: CompletableDeferred<Unit>? = null
        var pageGate: CompletableDeferred<Unit>? = null
        var ignoreCalendarCancellation = false
        var ignorePageCancellation = false

        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            periods += period
            val result = calendarResult
            val gate = calendarGate
            if (ignoreCalendarCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }

        override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> {
            pages += page to pageSize
            val result = pageResult
            val gate = pageGate
            if (ignorePageCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
    }

    private fun viewModel(repository: Repository = Repository(), clock: Clock = Clock(), loggedIn: Boolean = true) =
        CheckInHistoryViewModel(repository, clock, loggedIn).also { store.put("history", it) }

    @Test fun initialLoadIsCalendarOnlyAndCachedModesDoNotReload() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        runCurrent()
        assertEquals(1, repository.periods.size)
        assertTrue(repository.pages.isEmpty())
        assertTrue(vm.state.value.calendar.hasLoaded)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.Calendar))
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        assertEquals(listOf(1 to 20), repository.pages)
        assertEquals(1, repository.periods.size)
    }

    @Test fun immediatelyCompletedLoadsDoNotLeaveAStuckJobGuard() = runTest(dispatcher) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = Repository()
        val vm = viewModel(repository)
        assertTrue(vm.state.value.calendar.hasLoaded)
        assertFalse(vm.state.value.calendar.isLoading)
        vm.onAction(CheckInHistoryAction.Refresh)
        assertEquals(2, repository.periods.size)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        repository.pageResult = Result.success(page(2, "next"))
        vm.onAction(CheckInHistoryAction.LoadMore)
        assertEquals(listOf(1 to 20, 2 to 20), repository.pages)
        assertFalse(vm.state.value.list.isLoadingMore)
    }

    @Test fun guestNeverReadsPrivateHistoryOrAcceptsSameGenerationLogin() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository, loggedIn = false)
        vm.onAction(CheckInHistoryAction.Refresh)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        vm.onAction(CheckInHistoryAction.LoadMore)
        vm.onAction(CheckInHistoryAction.SessionChanged(0, true))
        runCurrent()
        assertTrue(repository.periods.isEmpty())
        assertTrue(repository.pages.isEmpty())
        assertFalse(vm.state.value.isLoggedIn)
        assertTrue(vm.state.value.list.checkIns.isEmpty())
        assertFalse(vm.state.value.calendar.hasError)
    }

    @Test fun calendarGroupsLocalDatesCountsUniqueShopsAndDeduplicatesEntries() = runTest(dispatcher) {
        val boundary = "2026-09-30T23:00:00Z"
        val clock = Clock().apply { context = Calendar(dates = mapOf(boundary to "2026-10-01")) }
        val first = entry("one", boundary)
        val repository = Repository().apply {
            calendarResult = Result.success(listOf(first, first, entry("two", "2026-10-03T10:00:00Z"),
                entry("removed", "2026-10-03T11:00:00Z").copy(shop = null), entry("outside", "2026-09-29T10:00:00Z")))
        }
        val vm = viewModel(repository, clock)
        runCurrent()
        val state = vm.state.value.calendar
        assertEquals(setOf("2026-10-01", "2026-10-03"), state.checkInsByDate.keys)
        assertEquals(3, state.visitCount)
        assertEquals(1, state.shopCount)
        assertEquals("2026-10-03", state.selectedDate)
        assertEquals(listOf("two", "removed"), state.selectedCheckIns.map { it.id })
    }

    @Test fun explicitDateDuringLoadAndLaterRefreshIsNotOverwritten() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { calendarGate = gate }
        val vm = viewModel(repository)
        runCurrent()
        vm.onAction(CheckInHistoryAction.SelectDate("2026-10-05"))
        vm.onAction(CheckInHistoryAction.SelectDate("2026-09-05"))
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals("2026-10-05", vm.state.value.calendar.selectedDate)
        vm.onAction(CheckInHistoryAction.Refresh)
        runCurrent()
        assertEquals("2026-10-05", vm.state.value.calendar.selectedDate)
    }

    @Test fun monthNavigationRejectsFutureOverflowAndClearsOldMonthContent() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        runCurrent()
        vm.onAction(CheckInHistoryAction.ChangeMonth(1))
        vm.onAction(CheckInHistoryAction.ChangeMonth(Int.MIN_VALUE))
        runCurrent()
        assertEquals(1, repository.periods.size)
        repository.calendarGate = CompletableDeferred()
        vm.onAction(CheckInHistoryAction.ChangeMonth(-1))
        runCurrent()
        assertEquals(HistoryMonth(2026, 9), vm.state.value.calendar.month)
        assertTrue(vm.state.value.calendar.checkInsByDate.isEmpty())
        assertNull(vm.state.value.calendar.selectedDate)
        assertTrue(vm.state.value.canGoNextMonth)
        assertEquals("2026-09-01T00:00:00Z", repository.periods.last().fromInclusive)
        repository.calendarGate!!.complete(Unit)
        runCurrent()
    }

    @Test fun lateCancelledMonthCannotOverwriteNewMonthOrItsLoadingFlags() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        val current = CompletableDeferred<Unit>()
        val repository = Repository().apply { calendarGate = old; ignoreCalendarCancellation = true }
        val vm = viewModel(repository)
        runCurrent()
        repository.calendarGate = current
        repository.calendarResult = Result.success(listOf(entry("september", "2026-09-05T10:00:00Z")))
        vm.onAction(CheckInHistoryAction.ChangeMonth(-1))
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.calendar.isLoading)
        assertTrue(vm.state.value.calendar.checkInsByDate.isEmpty())
        current.complete(Unit)
        runCurrent()
        assertEquals(listOf("september"), vm.state.value.calendar.selectedCheckIns.map { it.id })
        assertFalse(vm.state.value.calendar.isLoading)
    }

    @Test fun calendarRefreshFailureRetainsDataAndExplicitRetryRecovers() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        runCurrent()
        repository.calendarResult = Result.failure(IllegalStateException("secret server message"))
        vm.onAction(CheckInHistoryAction.Refresh)
        runCurrent()
        assertTrue(vm.state.value.calendar.hasLoaded)
        assertTrue(vm.state.value.calendar.hasError)
        assertEquals(1, vm.state.value.calendar.visitCount)
        repository.calendarResult = Result.success(emptyList())
        vm.onAction(CheckInHistoryAction.Retry)
        runCurrent()
        assertFalse(vm.state.value.calendar.hasError)
        assertEquals(0, vm.state.value.calendar.visitCount)
        assertEquals("2026-10-14", vm.state.value.calendar.selectedDate)
    }

    @Test fun listPagingIsGuardedAndDeduplicatesWithoutLosingServerPageMetadata() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.pageGate = gate
        repository.pageResult = Result.success(page(2, "first", "second"))
        repeat(3) { vm.onAction(CheckInHistoryAction.LoadMore) }
        runCurrent()
        assertEquals(listOf(1 to 20, 2 to 20), repository.pages)
        assertTrue(vm.state.value.list.isLoadingMore)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("first", "second"), vm.state.value.list.checkIns.map { it.id })
        assertEquals(2, vm.state.value.list.page)
        assertTrue(vm.state.value.list.hasMore)
    }

    @Test fun pagingFailureRequiresExplicitRetryAndKeepsCurrentPageAndContent() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        repository.pageResult = Result.failure(IllegalStateException("offline"))
        vm.onAction(CheckInHistoryAction.LoadMore)
        runCurrent()
        assertEquals(1, vm.state.value.list.page)
        assertEquals(listOf("first"), vm.state.value.list.checkIns.map { it.id })
        assertTrue(vm.state.value.list.hasLoadMoreError)
        assertFalse(vm.state.value.list.hasError)
        repeat(3) { vm.onAction(CheckInHistoryAction.LoadMore) }
        runCurrent()
        assertEquals(2, repository.pages.size)
        repository.pageResult = Result.success(page(2, "next"))
        vm.onAction(CheckInHistoryAction.RetryLoadMore)
        runCurrent()
        assertEquals(listOf(1 to 20, 2 to 20, 2 to 20), repository.pages)
        assertEquals(2, vm.state.value.list.page)
        assertFalse(vm.state.value.list.hasLoadMoreError)
    }

    @Test fun resetCancelsPagingAndItsLateReplyNeverAppendsToRefreshedList() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        val old = CompletableDeferred<Unit>()
        repository.pageGate = old
        repository.ignorePageCancellation = true
        repository.pageResult = Result.success(page(2, "old-page"))
        vm.onAction(CheckInHistoryAction.LoadMore)
        runCurrent()
        repository.pageGate = null
        repository.pageResult = Result.success(page(1, "new-page"))
        vm.onAction(CheckInHistoryAction.Refresh)
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(listOf(1 to 20, 2 to 20, 1 to 20), repository.pages)
        assertEquals(listOf("new-page"), vm.state.value.list.checkIns.map { it.id })
        assertEquals(1, vm.state.value.list.page)
        assertFalse(vm.state.value.list.isLoadingMore)
    }

    @Test fun refreshSignalsDuringReadDiscardOldSnapshotAndCoalesceOneFollowUp() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        val repository = Repository().apply { calendarGate = old }
        val vm = viewModel(repository)
        runCurrent()
        repeat(3) { vm.onAction(CheckInHistoryAction.ContentChanged(it + 1)) }
        runCurrent()
        assertEquals(1, repository.periods.size)
        repository.calendarGate = null
        repository.calendarResult = Result.success(listOf(entry("after-write")))
        old.complete(Unit)
        runCurrent()
        assertEquals(2, repository.periods.size)
        assertEquals(listOf("after-write"), vm.state.value.calendar.selectedCheckIns.map { it.id })
        vm.onAction(CheckInHistoryAction.ContentChanged(3))
        vm.onAction(CheckInHistoryAction.ContentChanged(1))
        runCurrent()
        assertEquals(2, repository.periods.size)
    }

    @Test fun firstResumeDoesNotDuplicateLoadAndLaterResumeInvalidatesBothModes() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        runCurrent()
        vm.onAction(CheckInHistoryAction.Resume)
        runCurrent()
        assertEquals(1, repository.periods.size)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        vm.onAction(CheckInHistoryAction.Resume)
        runCurrent()
        assertEquals(2, repository.pages.size)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.Calendar))
        runCurrent()
        assertEquals(2, repository.periods.size)
    }

    @Test fun firstResumeDoesNotAutomaticallyRetryAnAlreadyFailedInitialRead() = runTest(dispatcher) {
        val repository = Repository().apply {
            calendarResult = Result.failure(IllegalStateException("offline"))
            pageResult = Result.failure(IllegalStateException("offline"))
        }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        vm.onAction(CheckInHistoryAction.Resume)
        runCurrent()
        assertEquals(1, repository.periods.size)
        assertEquals(1, repository.pages.size)
        assertTrue(vm.state.value.list.hasError)
    }

    @Test fun invalidatedInactiveCalendarDoesNotPublishOrRefetchUntilSelected() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        val repository = Repository().apply { calendarGate = old }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        repository.calendarGate = null
        repository.calendarResult = Result.success(listOf(entry("fresh-calendar")))
        vm.onAction(CheckInHistoryAction.ContentChanged(1))
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(1, repository.periods.size)
        assertTrue(vm.state.value.calendar.checkInsByDate.isEmpty())
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.Calendar))
        runCurrent()
        assertEquals(2, repository.periods.size)
        assertEquals(listOf("fresh-calendar"), vm.state.value.calendar.selectedCheckIns.map { it.id })
    }

    @Test fun clearingLifecycleStoreCancelsBothLoadsWithoutExecutingQueuedRefresh() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { calendarGate = gate; pageGate = gate }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        vm.onAction(CheckInHistoryAction.ContentChanged(1))
        runCurrent()
        store.clear()
        runCurrent()
        assertEquals(1, repository.periods.size)
        assertEquals(1, repository.pages.size)
        assertFalse(vm.state.value.calendar.isLoading)
        assertFalse(vm.state.value.list.isLoading)
        assertFalse(vm.state.value.calendar.hasError)
        assertFalse(vm.state.value.list.hasError)
    }

    @Test fun logoutClearsBothModesAndRejectsRepliesEvenIfTransportIgnoresCancellation() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        val repository = Repository().apply {
            calendarGate = old; pageGate = old
            ignoreCalendarCancellation = true; ignorePageCancellation = true
        }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        vm.onAction(CheckInHistoryAction.SessionChanged(1, false))
        runCurrent()
        assertEquals(1, vm.state.value.sessionGeneration)
        assertFalse(vm.state.value.isLoggedIn)
        assertTrue(vm.state.value.calendar.checkInsByDate.isEmpty())
        assertTrue(vm.state.value.list.checkIns.isEmpty())
        old.complete(Unit)
        runCurrent()
        vm.onAction(CheckInHistoryAction.Refresh)
        vm.onAction(CheckInHistoryAction.SessionChanged(0, true))
        runCurrent()
        assertEquals(1, repository.periods.size)
        assertEquals(1, repository.pages.size)
        assertTrue(vm.state.value.list.checkIns.isEmpty())
    }

    @Test fun newAccountRetainsModeButNotPreviousEntriesAndIgnoresOldGeneration() = runTest(dispatcher) {
        val repository = Repository()
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        val next = CompletableDeferred<Unit>()
        repository.pageGate = next
        repository.pageResult = Result.success(page(1, "new-account"))
        vm.onAction(CheckInHistoryAction.SessionChanged(2, true))
        runCurrent()
        assertEquals(2, vm.state.value.sessionGeneration)
        assertEquals(CheckInHistoryMode.List, vm.state.value.mode)
        assertTrue(vm.state.value.list.checkIns.isEmpty())
        assertTrue(vm.state.value.calendar.checkInsByDate.isEmpty())
        vm.onAction(CheckInHistoryAction.SessionChanged(1, false))
        vm.onAction(CheckInHistoryAction.SessionChanged(2, true))
        runCurrent()
        assertEquals(2, repository.pages.size)
        next.complete(Unit)
        runCurrent()
        assertEquals(listOf("new-account"), vm.state.value.list.checkIns.map { it.id })
    }

    @Test fun listRefreshFailureRetainsEntriesAndEndOfPaginationDoesNotRequestAgain() = runTest(dispatcher) {
        val repository = Repository().apply { pageResult = Result.success(page(1, "only").copy(totalCount = 1)) }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        vm.onAction(CheckInHistoryAction.LoadMore)
        runCurrent()
        assertEquals(1, repository.pages.size)
        repository.pageResult = Result.failure(IllegalStateException("offline"))
        vm.onAction(CheckInHistoryAction.Refresh)
        runCurrent()
        assertTrue(vm.state.value.list.hasError)
        assertTrue(vm.state.value.list.hasLoaded)
        assertEquals(listOf("only"), vm.state.value.list.checkIns.map { it.id })
        assertFalse(vm.state.value.list.isLoading)
    }

    @Test fun clockRolloverUpdatesTodayAndClampsANowFutureMonth() = runTest(dispatcher) {
        val clock = Clock().apply { context = Calendar(today = "2026-11-01") }
        val repository = Repository()
        val vm = viewModel(repository, clock)
        runCurrent()
        clock.context = Calendar(today = "2026-10-31")
        vm.onAction(CheckInHistoryAction.Resume)
        runCurrent()
        assertEquals("2026-10-31", vm.state.value.today)
        assertEquals(HistoryMonth(2026, 10), vm.state.value.calendar.month)
        assertFalse(vm.state.value.canGoNextMonth)
        assertEquals(2, repository.periods.size)
    }

    @Test fun zoneChangeUsesOneNewSnapshotForBoundsAndGroupingAndDropsOldReply() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        val clock = Clock()
        val repository = Repository().apply { calendarGate = old; ignoreCalendarCancellation = true }
        val vm = viewModel(repository, clock)
        runCurrent()
        val instant = "2026-09-30T22:00:00Z"
        val bounds = HistoryPeriod("2026-09-30T21:00:00Z", "2026-10-31T21:00:00Z")
        clock.context = Calendar(zoneId = "Europe/Minsk", dates = mapOf(instant to "2026-10-01"), bounds = bounds)
        repository.calendarGate = null
        repository.calendarResult = Result.success(listOf(entry("local-visit", instant)))
        vm.onAction(CheckInHistoryAction.Resume)
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(bounds, repository.periods.last())
        assertEquals(listOf("local-visit"), vm.state.value.calendar.selectedCheckIns.map { it.id })
        assertEquals(setOf("2026-10-01"), vm.state.value.calendar.checkInsByDate.keys)
    }

    @Test fun clockFailureDoesNotQueryOldZoneAndExplicitRetryUsesRecoveredContext() = runTest(dispatcher) {
        val clock = Clock()
        val repository = Repository()
        val vm = viewModel(repository, clock)
        runCurrent()
        clock.fails = true
        vm.onAction(CheckInHistoryAction.Refresh)
        runCurrent()
        assertEquals(1, repository.periods.size)
        assertTrue(vm.state.value.calendar.hasError)
        assertEquals(1, vm.state.value.calendar.visitCount)
        clock.fails = false
        clock.context = Calendar(zoneId = "another-zone")
        vm.onAction(CheckInHistoryAction.Retry)
        runCurrent()
        assertEquals(2, repository.periods.size)
        assertFalse(vm.state.value.calendar.hasError)
    }

    @Test fun cancellationRemainsCancellationWithoutErrorFlagsOrAutomaticRetry() = runTest(dispatcher) {
        val repository = Repository().apply {
            calendarResult = Result.failure(CancellationException("cancel"))
            pageResult = Result.failure(CancellationException("cancel"))
        }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        assertFalse(vm.state.value.calendar.hasError)
        assertFalse(vm.state.value.list.hasError)
        assertFalse(vm.state.value.calendar.isLoading)
        assertFalse(vm.state.value.list.isLoading)
        assertEquals(1, repository.periods.size)
        assertEquals(1, repository.pages.size)
    }

    @Test fun malformedCalendarDatesAndUnexpectedPagesBecomeExplicitLoadErrors() = runTest(dispatcher) {
        val repository = Repository().apply {
            calendarResult = Result.success(listOf(entry("bad-date", "not-a-date")))
            pageResult = Result.success(page(2, "wrong-page"))
        }
        val vm = viewModel(repository)
        vm.onAction(CheckInHistoryAction.SelectMode(CheckInHistoryMode.List))
        runCurrent()
        assertTrue(vm.state.value.calendar.hasError)
        assertTrue(vm.state.value.list.hasError)
        assertTrue(vm.state.value.list.checkIns.isEmpty())
    }

    companion object {
        private fun page(number: Int, vararg ids: String) = CheckInHistoryPage(ids.map { entry(it) }, 41, number, 20)
        private fun entry(id: String, at: String = "2026-10-14T10:00:00Z") = PersonalCheckIn(
            id, HistoryAddress("shop", "/coffee-shop/shop"), "Coffee", null, "Owner", "Private note",
            at, at, null, emptyList(), null, null, HistoryVisibility.Private, HistoryModeration.NotSubmitted,
            0, null, 0, false,
        )
    }
}
