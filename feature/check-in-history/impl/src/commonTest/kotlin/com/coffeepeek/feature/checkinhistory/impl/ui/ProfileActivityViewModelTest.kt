package com.coffeepeek.feature.checkinhistory.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val shops = ActivityShops()
    private val homes = ActivityHomes()
    private val clock = ActivityClock()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model(weekly: Boolean = false, loggedIn: Boolean = true): ProfileActivityViewModel {
        val model = ProfileActivityViewModel(shops, homes, clock::snapshot, weekly, loggedIn)
        store.put(if (weekly) "week" else "month", model)
        runCurrent()
        return model
    }

    @Test fun monthUsesExclusiveShopInstantsAndInclusiveHomeDatesWithoutShopPages() = runTest(dispatcher) {
        val model = model()
        assertEquals(HistoryPeriod("2026-10-01T00:00:00Z", "2026-11-01T00:00:00Z"), shops.periods.single())
        assertEquals(HomeHistoryDateRange("2026-10-01", "2026-10-31"), homes.ranges.single())
        assertEquals(listOf(1), homes.pages.map { it.first })
        assertFalse(model.state.value.loading)
        assertFalse(model.state.value.failed)
        assertNull(model.state.value.calendar.selectedDate)
    }

    @Test fun completeWeekCrossesMonthAndYearWhileMonthBrowsingIsIndependent() = runTest(dispatcher) {
        clock.today = "2027-01-01"
        val week = model(weekly = true)
        assertEquals("2026-12-28", week.state.value.weekDates.first())
        assertEquals("2027-01-03", week.state.value.weekDates.last())
        assertEquals(HistoryPeriod("2026-12-28T00:00:00Z", "2027-01-04T00:00:00Z"), shops.periods.single())
        val month = model()
        month.onAction(ProfileActivityAction.ChangeMonth(-1))
        runCurrent()
        assertEquals(HistoryMonth(2026, 12), month.state.value.calendar.month)
        assertEquals(HistoryMonth(2027, 1), week.state.value.calendar.month)
    }

    @Test fun guestDoesNotReadPrivateDataOrEmitAnOpenEvent() = runTest(dispatcher) {
        val model = model(loggedIn = false)
        assertTrue(shops.periods.isEmpty())
        assertTrue(homes.ranges.isEmpty())
        model.onAction(ProfileActivityAction.Retry)
        runCurrent()
        assertTrue(shops.periods.isEmpty())
        assertTrue(model.state.value.calendar.calendarCounts.isEmpty())
    }

    @Test fun dayGroupsVisitsFirstThenHomesAndUsesInstantsWithinEachSource() = runTest(dispatcher) {
        shops.load = { Result.success(listOf(activityShop("late"), activityShop("early", "2026-10-25T11:00:00+03:00"))) }
        homes.days = Result.success(listOf(HomeHistoryDay("2026-10-25", 1)))
        homes.load = { page, size, _ -> Result.success(HomeHistoryPage(listOf(activityHome("home")), 1, page, size)) }
        val model = model()
        model.onAction(ProfileActivityAction.SelectDate("2026-10-25"))
        runCurrent()
        assertEquals(listOf("shop:early", "shop:late", "home:home"), model.state.value.selectedRows.map { it.key })
        assertEquals(3L, model.state.value.calendar.calendarCounts["2026-10-25"])
        model.onAction(ProfileActivityAction.SelectDate("2026-10-25"))
        runCurrent()
        assertNull(model.state.value.calendar.selectedDate)
    }

    @Test fun futureDatesMonthsAndOtherMonthSelectionsAreRejected() = runTest(dispatcher) {
        val model = model()
        model.onAction(ProfileActivityAction.ChangeMonth(1))
        model.onAction(ProfileActivityAction.SelectDate("2026-10-26"))
        model.onAction(ProfileActivityAction.SelectDate("2026-09-01"))
        runCurrent()
        assertEquals(HistoryMonth(2026, 10), model.state.value.calendar.month)
        assertNull(model.state.value.calendar.selectedDate)
        assertEquals(1, shops.periods.size)
    }

    @Test fun shopSelectionOpensItsPublicSlugAndHomeSelectionOpensHistory() = runTest(dispatcher) {
        shops.load = { Result.success(listOf(activityShop("visit"))) }
        homes.load = { page, size, _ -> Result.success(HomeHistoryPage(listOf(activityHome("home")), 1, page, size)) }
        val model = model()
        model.onAction(ProfileActivityAction.SelectDate("2026-10-25"))
        runCurrent()
        val shop = async(start = CoroutineStart.UNDISPATCHED) { model.events.first() }
        model.onAction(ProfileActivityAction.OpenRow("shop:visit"))
        runCurrent()
        assertEquals(ProfileActivityEvent.OpenShop("coffee"), shop.await().event)
        val home = async(start = CoroutineStart.UNDISPATCHED) { model.events.first() }
        model.onAction(ProfileActivityAction.OpenRow("home:home"))
        runCurrent()
        assertEquals(ProfileActivityEvent.OpenHistory, home.await().event)
    }

    @Test fun sourceFailureIsRetriedWithoutDiscardingTheOtherCalendar() = runTest(dispatcher) {
        shops.load = { Result.success(listOf(activityShop("visit"))) }
        homes.days = Result.failure(IllegalStateException("offline"))
        val model = model()
        assertTrue(model.state.value.failed)
        assertEquals(1L, model.state.value.calendar.calendarCounts["2026-10-25"])
        homes.days = Result.success(listOf(HomeHistoryDay("2026-10-25", 2)))
        model.onAction(ProfileActivityAction.Retry)
        runCurrent()
        assertFalse(model.state.value.failed)
        assertEquals(3L, model.state.value.calendar.calendarCounts["2026-10-25"])
    }

    @Test fun initialResumeDoesNotDuplicateReadsAndReturnRefreshesUsingNewToday() = runTest(dispatcher) {
        val model = model(weekly = true)
        model.onAction(ProfileActivityAction.Resume)
        runCurrent()
        assertEquals(1, shops.periods.size)
        clock.today = "2026-10-26"
        model.onAction(ProfileActivityAction.Resume)
        runCurrent()
        assertEquals(2, shops.periods.size)
        assertEquals("2026-10-26", model.state.value.weekDates.first())
    }

    @Test fun todayReturnsToCurrentMonthAndSelectsTheDay() = runTest(dispatcher) {
        val model = model()
        model.onAction(ProfileActivityAction.ChangeMonth(-1))
        runCurrent()
        clock.today = "2026-11-01"
        model.onAction(ProfileActivityAction.Today)
        runCurrent()
        assertEquals(HistoryMonth(2026, 11), model.state.value.calendar.month)
        assertEquals("2026-11-01", model.state.value.calendar.selectedDate)
    }

    @Test fun clockFailureStopsLoadingAndRetryCanRecover() = runTest(dispatcher) {
        val model = model()
        clock.failed = true
        model.onAction(ProfileActivityAction.Retry)
        runCurrent()
        assertTrue(model.state.value.failed)
        assertFalse(model.state.value.loading)
        clock.failed = false
        model.onAction(ProfileActivityAction.Retry)
        runCurrent()
        assertFalse(model.state.value.failed)
    }

    @Test fun aNonCooperativeOldMonthCannotReplaceNewerData() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = model()
        shops.load = { period ->
            if (period.fromInclusive.startsWith("2026-09")) {
                withContext(NonCancellable) { gate.await() }
                Result.success(listOf(activityShop("stale", "2026-09-01T10:00:00Z")))
            } else Result.success(listOf(activityShop("current")))
        }
        model.onAction(ProfileActivityAction.ChangeMonth(-1))
        runCurrent()
        model.onAction(ProfileActivityAction.Today)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("current"), model.state.value.calendar.shopCalendar.content.values.flatten().map { it.entry.id })
    }

    @Test fun cancellationIsNotReportedAsACalendarError() = runTest(dispatcher) {
        shops.load = { throw CancellationException("cancelled") }
        val model = model()
        assertFalse(model.state.value.failed)
        store.clear()
        runCurrent()
        assertEquals(1, shops.periods.size)
    }

    @Test fun bufferedNavigationBelongsOnlyToTheSurfaceThatRequestedIt() = runTest(dispatcher) {
        val model = model(weekly = true)
        val previous = Any()
        model.onAction(ProfileActivityAction.OpenWeek(previous))
        runCurrent()
        val event = model.events.first()
        assertEquals(ProfileActivityEvent.OpenWeek, event.forOwner(previous))
        assertNull(event.forOwner(Any()))
    }
}
