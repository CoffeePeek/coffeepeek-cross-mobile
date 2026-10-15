package com.coffeepeek.feature.checkinhistory.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryAddress
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDay
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryRecipe
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryKindFilter
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
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
class CombinedHistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val gates = mutableListOf<CompletableDeferred<Unit>>()
    private fun gate() = CompletableDeferred<Unit>().also(gates::add)
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { gates.forEach { it.complete(Unit) }; store.clear(); Dispatchers.resetMain() }

    private class Calendar(override val today: String = "2026-10-16", override val zoneId: String = "UTC",
        private val dates: Map<String, String> = emptyMap(), private val bounds: HistoryPeriod? = null) : HistoryCalendarContext {
        override fun localDate(instant: String) = dates[instant]
            ?: instant.substringBefore('T').takeIf { HistoryMonth.fromDate(it) != null }
        override fun period(month: HistoryMonth) = bounds
            ?: HistoryPeriod("${month.date(1)}T00:00:00Z", "${month.plusMonths(1).date(1)}T00:00:00Z")
    }
    private class Clock : HistoryCalendarProvider {
        var context: HistoryCalendarContext = Calendar()
        var fails = false
        override fun snapshot(): HistoryCalendarContext { check(!fails) { "Private clock error" }; return context }
    }
    private class Shops : CheckInHistoryRepository {
        val pages = mutableListOf<Int>()
        val periods = mutableListOf<HistoryPeriod>()
        var pageResult: (Int) -> Result<CheckInHistoryPage> = { Result.success(shopPage(it, "shop-$it")) }
        var calendarResult = Result.success(listOf(shop("calendar")))
        var pageGate: CompletableDeferred<Unit>? = null
        var calendarGate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> {
            pages += page
            val result = pageResult(page)
            val gate = pageGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            periods += period
            val result = calendarResult
            val gate = calendarGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
    }
    private class Homes : HomeHistoryReader {
        val pages = mutableListOf<Pair<Int, HomeHistoryDateRange?>>()
        val ranges = mutableListOf<HomeHistoryDateRange>()
        var pageResult: (Int, HomeHistoryDateRange?) -> Result<HomeHistoryPage> = { number, range ->
            Result.success(homePage(number, home("home-$number", range?.from ?: "2026-10-16")))
        }
        var calendarResult = Result.success(listOf(HomeHistoryDay("2026-10-16", 2)))
        var pageGate: CompletableDeferred<Unit>? = null
        var calendarGate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage> {
            pages += page to range
            val result = pageResult(page, range)
            val gate = pageGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
        override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> {
            ranges += range
            val result = calendarResult
            val gate = calendarGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
    }
    private fun vm(shops: Shops = Shops(), homes: Homes = Homes(), clock: Clock = Clock(), loggedIn: Boolean = true,
        contentGeneration: Int = 0) = CombinedHistoryViewModel(shops, homes, clock, loggedIn,
            initialContentGeneration = contentGeneration).also { store.put("combined", it) }

    @Test fun initialLoadCombinesFourIndependentReadsAndKeepsSourcePrefixedIdentity() = runTest(dispatcher) {
        val shops = Shops().apply { pageResult = { Result.success(shopPage(it, "same")) } }
        val homes = Homes().apply { pageResult = { number, _ -> Result.success(homePage(number, home("same"))) } }
        val vm = vm(shops, homes)
        runCurrent()
        assertNull(vm.state.value.selectedDate)
        assertEquals(listOf("shop:same", "home:same"), vm.state.value.timeline.map { it.key })
        assertEquals(mapOf("2026-10-16" to 3L), vm.state.value.calendarCounts)
        assertEquals(listOf(1), shops.pages)
        assertEquals(listOf<Pair<Int, HomeHistoryDateRange?>>(1 to null), homes.pages)
        assertEquals(HomeHistoryDateRange("2026-10-01", "2026-10-31"), homes.ranges.single())
        assertFalse(vm.state.value.isTimelineLoading)
    }

    @Test fun partialHomeFailureKeepsShopsAndRetryDoesNotReloadSuccessfulSources() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes().apply { pageResult = { _, _ -> Result.failure(IllegalStateException("private server detail")) } }
        val vm = vm(shops, homes)
        runCurrent()
        assertEquals(listOf("shop:shop-1"), vm.state.value.timeline.map { it.key })
        assertTrue(vm.state.value.homes.hasError)
        assertFalse(vm.state.value.shops.hasError)
        homes.pageResult = { number, _ -> Result.success(homePage(number, home("recovered"))) }
        vm.onAction(CombinedHistoryAction.RetryTimeline(HistorySource.Home))
        runCurrent()
        assertFalse(vm.state.value.homes.hasError)
        assertEquals(2, vm.state.value.timeline.size)
        assertEquals(1, shops.pages.size)
        assertEquals(1, shops.periods.size)
        assertEquals(1, homes.ranges.size)
    }

    @Test fun calendarErrorDoesNotHideUnfilteredTimelineOrRestartItsPage() = runTest(dispatcher) {
        val shops = Shops().apply { calendarResult = Result.failure(IllegalStateException("calendar down")) }
        val vm = vm(shops)
        runCurrent()
        assertEquals(2, vm.state.value.timeline.size)
        assertTrue(vm.state.value.shopCalendar.hasError)
        shops.calendarResult = Result.success(listOf(shop("fixed")))
        vm.onAction(CombinedHistoryAction.RetryCalendar(HistorySource.Shop))
        runCurrent()
        assertEquals(2, shops.periods.size)
        assertEquals(1, shops.pages.size)
        assertFalse(vm.state.value.shopCalendar.hasError)
    }

    @Test fun completedFilterSwitchesReuseTheirOwnPagesAndCalendarWithoutExtraRequests() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.Home))
        runCurrent()
        assertEquals(listOf("home:home-1"), vm.state.value.timeline.map { it.key })
        assertEquals(mapOf("2026-10-16" to 2L), vm.state.value.calendarCounts)
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.Shop))
        runCurrent()
        assertEquals(mapOf("2026-10-16" to 1L), vm.state.value.calendarCounts)
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.All))
        runCurrent()
        assertEquals(1, shops.pages.size)
        assertEquals(1, homes.pages.size)
    }

    @Test fun hiddenInFlightSourceIsCanceledAndLateReplyCannotReplaceItsNewSnapshot() = runTest(dispatcher) {
        val old = gate()
        val homes = Homes().apply { pageGate = old; ignoreCancellation = true }
        val vm = vm(homes = homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.Shop))
        runCurrent()
        assertFalse(vm.state.value.homes.isLoading)
        homes.pageGate = null
        homes.pageResult = { number, _ -> Result.success(homePage(number, home("new"))) }
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.All))
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(listOf("home:new"), vm.state.value.homes.items.map { it.key })
        assertEquals(2, homes.pages.size)
    }

    @Test fun dateSelectionUsesShopMonthSnapshotAndHomeDayPagingWithIndependentCursor() = runTest(dispatcher) {
        val shops = Shops().apply { calendarResult = Result.success(listOf(shop("day", "2026-10-10T10:00:00Z"))) }
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SelectDate("2026-10-10"))
        runCurrent()
        val day = HomeHistoryDateRange("2026-10-10", "2026-10-10")
        assertEquals(listOf("shop:day", "home:home-1"), vm.state.value.timeline.map { it.key })
        assertEquals(1 to day, homes.pages.last())
        vm.onAction(CombinedHistoryAction.LoadMore)
        runCurrent()
        assertEquals(listOf(1), shops.pages)
        assertEquals(2 to day, homes.pages.last())
        assertEquals(2, vm.state.value.homes.page)
    }

    @Test fun clearingDateRestoresUnfilteredShopPagesAndResetsOnlyHomeQuery() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SelectDate("2026-10-10"))
        runCurrent()
        vm.onAction(CombinedHistoryAction.ChangeMonth(0))
        vm.onAction(CombinedHistoryAction.SelectDate("2026-09-10"))
        runCurrent()
        assertEquals("2026-10-10", vm.state.value.selectedDate)
        vm.onAction(CombinedHistoryAction.SelectDate("2026-10-10"))
        runCurrent()
        assertNull(vm.state.value.selectedDate)
        assertEquals(listOf(1), shops.pages)
        assertEquals(1 to null, homes.pages.last())
        assertEquals(listOf("shop:shop-1", "home:home-1"), vm.state.value.timeline.map { it.key })
    }

    @Test fun changingDateRejectsLateHomeRepliesWithoutClearingTheNewLoadingGuard() = runTest(dispatcher) {
        val homes = Homes()
        val vm = vm(homes = homes)
        runCurrent()
        val old = gate()
        val next = gate()
        homes.ignoreCancellation = true
        homes.pageGate = old
        vm.onAction(CombinedHistoryAction.SelectDate("2026-10-10"))
        runCurrent()
        homes.pageGate = next
        vm.onAction(CombinedHistoryAction.SelectDate("2026-10-11"))
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.homes.isLoading)
        assertTrue(vm.state.value.homes.items.isEmpty())
        next.complete(Unit)
        runCurrent()
        assertEquals("2026-10-11", vm.state.value.homes.items.single().date)
    }

    @Test fun oldMonthCannotOverwriteNewCalendarAndMonthNavigationDoesNotFilterTheAllDateTimeline() = runTest(dispatcher) {
        val old = gate()
        val shops = Shops().apply { calendarGate = old; ignoreCancellation = true }
        val homes = Homes().apply { calendarGate = old; ignoreCancellation = true }
        val vm = vm(shops, homes)
        runCurrent()
        val next = gate()
        shops.calendarGate = next
        shops.calendarResult = Result.success(listOf(shop("sep", "2026-09-10T10:00:00Z")))
        homes.calendarGate = next
        homes.calendarResult = Result.success(listOf(HomeHistoryDay("2026-09-10", 4)))
        vm.onAction(CombinedHistoryAction.ChangeMonth(-1))
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.shopCalendar.isLoading)
        assertTrue(vm.state.value.calendarCounts.isEmpty())
        next.complete(Unit)
        runCurrent()
        assertEquals(mapOf("2026-09-10" to 5L), vm.state.value.calendarCounts)
        assertEquals(2, vm.state.value.timeline.size)
        assertEquals(1, shops.pages.size)
        assertEquals(1, homes.pages.size)
    }

    @Test fun futureAndOverflowMonthsAreIgnoredAndTodayDoesNotToggleItselfOff() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.ChangeMonth(1))
        vm.onAction(CombinedHistoryAction.ChangeMonth(Int.MIN_VALUE))
        runCurrent()
        assertEquals(HistoryMonth(2026, 10), vm.state.value.month)
        assertEquals(1, shops.periods.size)
        vm.onAction(CombinedHistoryAction.Today)
        runCurrent()
        val reads = homes.pages.size
        vm.onAction(CombinedHistoryAction.Today)
        runCurrent()
        assertEquals("2026-10-16", vm.state.value.selectedDate)
        assertEquals(reads, homes.pages.size)
    }

    @Test fun paginationGuardsDeduplicateWithinSourcesButPreserveTwoKindsWithEqualIds() = runTest(dispatcher) {
        val shops = Shops().apply { pageResult = { Result.success(shopPage(it, "same")) } }
        val homes = Homes().apply { pageResult = { number, _ -> Result.success(homePage(number, home("same"))) } }
        val vm = vm(shops, homes)
        runCurrent()
        val next = gate()
        shops.pageGate = next; homes.pageGate = next
        repeat(3) { vm.onAction(CombinedHistoryAction.LoadMore) }
        runCurrent()
        assertEquals(listOf(1, 2), shops.pages)
        assertEquals(listOf(1, 2), homes.pages.map { it.first })
        next.complete(Unit)
        runCurrent()
        assertEquals(listOf("shop:same", "home:same"), vm.state.value.timeline.map { it.key })
        assertEquals(2, vm.state.value.shops.page)
        assertEquals(2, vm.state.value.homes.page)
    }

    @Test fun failedHomePaginationRequiresExplicitRetryWithoutBlockingOrReloadingShop() = runTest(dispatcher) {
        val shops = Shops().apply { pageResult = { Result.success(shopPage(it, "s-$it").copy(totalCount = 21)) } }
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        homes.pageResult = { _, _ -> Result.failure(IllegalStateException("offline")) }
        vm.onAction(CombinedHistoryAction.LoadMore)
        runCurrent()
        assertEquals(2, vm.state.value.shops.page)
        assertEquals(1, vm.state.value.homes.page)
        assertTrue(vm.state.value.homes.hasLoadMoreError)
        vm.onAction(CombinedHistoryAction.LoadMore)
        runCurrent()
        assertEquals(2, homes.pages.size)
        homes.pageResult = { number, _ -> Result.success(homePage(number, home("fixed"))) }
        vm.onAction(CombinedHistoryAction.RetryLoadMore(HistorySource.Home))
        runCurrent()
        assertEquals(listOf(1, 2, 2), homes.pages.map { it.first })
        assertEquals(listOf(1, 2), shops.pages)
        assertFalse(vm.state.value.homes.hasLoadMoreError)
    }

    @Test fun refreshCancelsBothPagingRequestsAndTheirLateResponsesCannotAppend() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        val old = gate()
        shops.pageGate = old; homes.pageGate = old
        shops.ignoreCancellation = true; homes.ignoreCancellation = true
        vm.onAction(CombinedHistoryAction.LoadMore)
        runCurrent()
        shops.pageGate = null; homes.pageGate = null
        shops.pageResult = { Result.success(shopPage(it, "new-shop")) }
        homes.pageResult = { number, _ -> Result.success(homePage(number, home("new-home"))) }
        vm.onAction(CombinedHistoryAction.Refresh)
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(listOf("shop:new-shop", "home:new-home"), vm.state.value.timeline.map { it.key })
        assertEquals(1, vm.state.value.shops.page)
        assertEquals(1, vm.state.value.homes.page)
        assertEquals(listOf(1, 2, 1), shops.pages)
    }

    @Test fun contentSignalsCoalesceExactlyOneFollowUpPerSourceEvenIfAnotherSourceFailsQuickly() = runTest(dispatcher) {
        val old = gate()
        val shops = Shops().apply { pageGate = old; calendarGate = old }
        val homes = Homes().apply { pageGate = old; calendarGate = old }
        val vm = vm(shops, homes, contentGeneration = 4)
        runCurrent()
        vm.onAction(CombinedHistoryAction.ContentChanged(4))
        runCurrent()
        repeat(3) { vm.onAction(CombinedHistoryAction.ContentChanged(it + 5)) }
        runCurrent()
        assertEquals(1, shops.pages.size)
        shops.pageGate = null; shops.calendarGate = null
        homes.pageGate = null; homes.calendarGate = null
        shops.pageResult = { Result.failure(IllegalStateException("quick follow-up failure")) }
        old.complete(Unit)
        runCurrent()
        assertEquals(2, shops.pages.size)
        assertEquals(2, shops.periods.size)
        assertEquals(2, homes.pages.size)
        assertEquals(2, homes.ranges.size)
        assertTrue(vm.state.value.shops.items.isEmpty())
        assertTrue(vm.state.value.shops.hasError)
        assertEquals(1, vm.state.value.homes.items.size)
        vm.onAction(CombinedHistoryAction.ContentChanged(6))
        runCurrent()
        assertEquals(2, shops.pages.size)
    }

    @Test fun sameQueryRefreshErrorsRetainBothSnapshotsAndTargetedRetryKeepsOtherError() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        val previous = vm.state.value.timeline
        shops.pageResult = { Result.failure(IllegalStateException("private error")) }
        homes.pageResult = { _, _ -> Result.failure(IllegalStateException("private error")) }
        vm.onAction(CombinedHistoryAction.Refresh)
        runCurrent()
        assertEquals(previous, vm.state.value.timeline)
        assertTrue(vm.state.value.shops.hasError)
        assertTrue(vm.state.value.homes.hasError)
        shops.pageResult = { Result.success(shopPage(it, "recovered")) }
        vm.onAction(CombinedHistoryAction.RetryTimeline(HistorySource.Shop))
        runCurrent()
        assertFalse(vm.state.value.shops.hasError)
        assertTrue(vm.state.value.homes.hasError)
        assertEquals(2, homes.pages.size)
    }

    @Test fun firstResumeReusesInitialLoadsIncludingFailuresAndSubsequentResumeRefreshes() = runTest(dispatcher) {
        val shops = Shops().apply { pageResult = { Result.failure(IllegalStateException("offline")) } }
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.Resume)
        runCurrent()
        assertEquals(1, shops.pages.size)
        assertEquals(1, homes.pages.size)
        vm.onAction(CombinedHistoryAction.Resume)
        runCurrent()
        assertEquals(2, shops.pages.size)
        assertEquals(2, homes.pages.size)
    }

    @Test fun zoneReplacementUsesOneNewSnapshotForBoundsAndGroupingAndDropsOldMonth() = runTest(dispatcher) {
        val old = gate()
        val shops = Shops().apply { calendarGate = old; ignoreCancellation = true }
        val clock = Clock()
        val vm = vm(shops, clock = clock)
        runCurrent()
        val instant = "2026-09-30T22:00:00Z"
        val bounds = HistoryPeriod("2026-09-30T21:00:00Z", "2026-10-31T21:00:00Z")
        clock.context = Calendar(zoneId = "Europe/Minsk", dates = mapOf(instant to "2026-10-01"), bounds = bounds)
        shops.calendarGate = null
        shops.calendarResult = Result.success(listOf(shop("local", instant)))
        vm.onAction(CombinedHistoryAction.Resume)
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(bounds, shops.periods.last())
        assertEquals(setOf("2026-10-01"), vm.state.value.shopCalendar.content.keys)
        assertEquals("2026-10-16", vm.state.value.homes.items.single().date)
    }

    @Test fun unavailableClockNeverQueriesOldZoneAndRefreshRecoversBothSources() = runTest(dispatcher) {
        val clock = Clock()
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes, clock)
        runCurrent()
        clock.fails = true
        vm.onAction(CombinedHistoryAction.Refresh)
        runCurrent()
        assertTrue(vm.state.value.isCalendarUnavailable)
        assertEquals(1, shops.periods.size)
        assertEquals(1, homes.pages.size)
        clock.fails = false
        vm.onAction(CombinedHistoryAction.Refresh)
        runCurrent()
        assertFalse(vm.state.value.isCalendarUnavailable)
        assertEquals(2, shops.pages.size)
        assertEquals(2, homes.pages.size)
    }

    @Test fun guestAndSameOrOlderSessionGenerationCannotReadOrExposePrivateContent() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes, loggedIn = false)
        vm.onAction(CombinedHistoryAction.Refresh)
        vm.onAction(CombinedHistoryAction.LoadMore)
        vm.onAction(CombinedHistoryAction.SessionChanged(0, true))
        vm.onAction(CombinedHistoryAction.SessionChanged(-1, true))
        runCurrent()
        assertTrue(shops.pages.isEmpty())
        assertTrue(homes.pages.isEmpty())
        assertTrue(vm.state.value.timeline.isEmpty())
        vm.onAction(CombinedHistoryAction.SessionChanged(1, true))
        runCurrent()
        assertEquals(2, vm.state.value.timeline.size)
    }

    @Test fun replacingAccountClearsAllSnapshotsAndRejectsNonCooperativeReplies() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        val old = gate()
        shops.pageGate = old; shops.calendarGate = old; shops.ignoreCancellation = true
        homes.pageGate = old; homes.calendarGate = old; homes.ignoreCancellation = true
        vm.onAction(CombinedHistoryAction.Refresh)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SessionChanged(2, false))
        runCurrent()
        assertTrue(vm.state.value.timeline.isEmpty())
        assertTrue(vm.state.value.calendarCounts.isEmpty())
        old.complete(Unit)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SessionChanged(1, true))
        runCurrent()
        assertFalse(vm.state.value.isLoggedIn)
        assertTrue(vm.state.value.shops.items.isEmpty())
        assertTrue(vm.state.value.homes.items.isEmpty())
    }

    @Test fun wrappedCancellationNeverBecomesAnErrorOrAutomaticRetry() = runTest(dispatcher) {
        val cancellation = CancellationException("Canceled")
        val shops = Shops().apply { pageResult = { Result.failure(cancellation) }; calendarResult = Result.failure(cancellation) }
        val homes = Homes().apply { pageResult = { _, _ -> Result.failure(cancellation) }; calendarResult = Result.failure(cancellation) }
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.Resume)
        runCurrent()
        assertFalse(vm.state.value.shops.hasError)
        assertFalse(vm.state.value.homes.hasError)
        assertFalse(vm.state.value.shopCalendar.hasError)
        assertFalse(vm.state.value.homeCalendar.hasError)
        assertFalse(vm.state.value.isTimelineLoading)
        assertEquals(1, shops.pages.size)
        assertEquals(1, homes.pages.size)
    }

    @Test fun newAccountKeepsFilterButItsFirstResumeDoesNotDuplicateStartupReads() = runTest(dispatcher) {
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.Resume)
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.Home))
        runCurrent()
        val next = gate()
        homes.pageGate = next
        homes.pageResult = { number, _ -> Result.success(homePage(number, home("new-account"))) }
        vm.onAction(CombinedHistoryAction.SessionChanged(1, true))
        runCurrent()
        assertEquals(HistoryKindFilter.Home, vm.state.value.filter)
        assertTrue(vm.state.value.timeline.isEmpty())
        vm.onAction(CombinedHistoryAction.Resume)
        runCurrent()
        assertEquals(2, homes.pages.size)
        next.complete(Unit)
        runCurrent()
        assertEquals(listOf("home:new-account"), vm.state.value.timeline.map { it.key })
        assertEquals(2, homes.pages.size)
        assertEquals(2, homes.ranges.size)
        assertTrue(vm.state.value.shops.items.isEmpty())
    }

    @Test fun unchangedAccountGenerationDoesNotCancelOrRestartPendingReads() = runTest(dispatcher) {
        val pending = gate()
        val shops = Shops().apply { pageGate = pending; calendarGate = pending }
        val homes = Homes().apply { pageGate = pending; calendarGate = pending }
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.SessionChanged(0, true))
        runCurrent()
        assertEquals(1, shops.pages.size)
        assertEquals(1, homes.pages.size)
        assertTrue(vm.state.value.isTimelineLoading)
        pending.complete(Unit)
        runCurrent()
        assertEquals(2, vm.state.value.timeline.size)
    }

    @Test fun invalidatedHiddenSourceWaitsForItsFilterBeforeFollowingUp() = runTest(dispatcher) {
        val pending = gate()
        val homes = Homes().apply { pageGate = pending; ignoreCancellation = true }
        val vm = vm(homes = homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.ContentChanged(1))
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.Shop))
        runCurrent()
        pending.complete(Unit)
        runCurrent()
        assertEquals(1, homes.pages.size)
        assertTrue(vm.state.value.homes.items.isEmpty())
        homes.pageGate = null
        vm.onAction(CombinedHistoryAction.SelectFilter(HistoryKindFilter.All))
        runCurrent()
        assertEquals(2, homes.pages.size)
    }

    @Test fun missingExpectedPagesFailExplicitlyAndLargeCalendarCountsDoNotOverflow() = runTest(dispatcher) {
        val shops = Shops().apply { pageResult = { Result.success(CheckInHistoryPage(emptyList(), 30, it, 20)) } }
        val homes = Homes().apply {
            pageResult = { number, _ -> Result.success(HomeHistoryPage(emptyList(), 30, number, 20)) }
            calendarResult = Result.success(listOf(HomeHistoryDay("2026-10-16", Int.MAX_VALUE)))
        }
        val vm = vm(shops, homes)
        runCurrent()
        assertTrue(vm.state.value.shops.hasError)
        assertTrue(vm.state.value.homes.hasError)
        assertEquals(Int.MAX_VALUE.toLong() + 1, vm.state.value.calendarCounts["2026-10-16"])
        homes.pageResult = { number, _ -> Result.success(HomeHistoryPage(emptyList(), 0, number, 20)) }
        vm.onAction(CombinedHistoryAction.RetryTimeline(HistorySource.Home))
        runCurrent()
        assertFalse(vm.state.value.homes.hasError)
        assertFalse(vm.state.value.homes.hasMore)
    }

    @Test fun clearingLifecycleStoreDoesNotRunQueuedFollowUps() = runTest(dispatcher) {
        val old = gate()
        val shops = Shops().apply { pageGate = old; calendarGate = old }
        val homes = Homes().apply { pageGate = old; calendarGate = old }
        val vm = vm(shops, homes)
        runCurrent()
        vm.onAction(CombinedHistoryAction.ContentChanged(1))
        runCurrent()
        store.clear()
        runCurrent()
        assertFalse(vm.state.value.isTimelineLoading)
        assertFalse(vm.state.value.shopCalendar.isLoading)
        assertEquals(1, shops.pages.size)
        assertEquals(1, homes.pages.size)
    }

    @Test fun runtimeMaskHidesBothKindsAndCalendarBeforeSessionActionIsReduced() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        val state = vm.state.value
        assertEquals(2, state.timeline.size)
        for (loggedIn in listOf(false, true)) {
            val masked = state.visibleTo(1, loggedIn)
            assertTrue(masked.timeline.isEmpty())
            assertTrue(masked.calendarCounts.isEmpty())
            assertTrue(masked.shops.items.isEmpty())
            assertTrue(masked.homes.items.isEmpty())
            assertFalse(masked.shops.hasLoaded)
        }
    }

    @Test fun malformedSourceMetadataAndWrongDayRowsBecomeSourceSpecificErrors() = runTest(dispatcher) {
        val shops = Shops().apply { pageResult = { Result.success(shopPage(3, "wrong-page")) } }
        val homes = Homes().apply { calendarResult = Result.success(listOf(HomeHistoryDay("2026-10-16", 1), HomeHistoryDay("2026-10-16", 2))) }
        val vm = vm(shops, homes)
        runCurrent()
        assertTrue(vm.state.value.shops.hasError)
        assertTrue(vm.state.value.homeCalendar.hasError)
        assertFalse(vm.state.value.homes.hasError)
        homes.pageResult = { number, _ -> Result.success(homePage(number, home("wrong-day", "2026-10-09"))) }
        vm.onAction(CombinedHistoryAction.SelectDate("2026-10-10"))
        runCurrent()
        assertTrue(vm.state.value.homes.hasError)
        assertTrue(vm.state.value.homes.items.isEmpty())
    }

    @Test fun immediateDispatchCompletesJobGuardsAndTodayRollBackClampsFutureMonth() = runTest(dispatcher) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clock = Clock().apply { context = Calendar(today = "2026-11-01") }
        val shops = Shops()
        val homes = Homes()
        val vm = vm(shops, homes, clock)
        assertFalse(vm.state.value.isTimelineLoading)
        vm.onAction(CombinedHistoryAction.Today)
        assertEquals("2026-11-01", vm.state.value.selectedDate)
        clock.context = Calendar(today = "2026-10-31")
        vm.onAction(CombinedHistoryAction.Refresh)
        assertEquals(HistoryMonth(2026, 10), vm.state.value.month)
        assertNull(vm.state.value.selectedDate)
        assertEquals(1 to null, homes.pages.last())
        vm.onAction(CombinedHistoryAction.LoadMore)
        assertEquals(2, vm.state.value.homes.page)
        assertFalse(vm.state.value.homes.isLoadingMore)
    }

    companion object {
        private fun shopPage(number: Int, vararg ids: String) = CheckInHistoryPage(ids.map { shop(it) }, 41, number, 20)
        private fun homePage(number: Int, vararg entries: HomeHistoryEntry) = HomeHistoryPage(entries.toList(), 41, number, 20)
        private fun shop(id: String, at: String = "2026-10-16T10:00:00Z") = PersonalCheckIn(
            id, HistoryAddress("shop", "/coffee-shop/shop"), "Coffee", null, "Owner", "Private note", at, at,
            null, emptyList(), null, null, HistoryVisibility.Private, HistoryModeration.NotSubmitted, 0, null, 0, false)
        private fun home(id: String, date: String = "2026-10-16") = HomeHistoryEntry(id, date,
            if (date == "2026-10-16") "2026-10-15T22:15:00Z" else "${date}T08:15:00Z", "${date}T08:00:00Z", null,
            HomeHistoryRecipe("drip", null, "Beans", "20", 120, 4), null, null, null, emptyList(), null,
            null, HistoryModeration.Unknown)
    }
}
