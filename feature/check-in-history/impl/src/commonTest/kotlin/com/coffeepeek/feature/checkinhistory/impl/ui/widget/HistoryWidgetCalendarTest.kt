package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import com.coffeepeek.feature.checkinhistory.domain.model.*
import kotlin.test.*

class HistoryWidgetCalendarTest {
    @Test fun weekIsOneMondayToSundayRowContainingToday() {
        val dates = widgetCalendarDates("2026-10-25", HistoryWidgetPeriod.Week)
        assertEquals(7, dates.size)
        assertEquals("2026-10-19", dates.first())
        assertEquals("2026-10-25", dates.last())
    }

    @Test fun fortnightIncludesThePreviousAndCurrentWeek() {
        val dates = widgetCalendarDates("2026-10-21", HistoryWidgetPeriod.Fortnight)
        assertEquals(14, dates.size)
        assertEquals("2026-10-12", dates.first())
        assertEquals("2026-10-25", dates.last())
        assertEquals(14, dates.toSet().size)
    }

    @Test fun weeksStayCompleteAcrossMonthAndYearBoundaries() {
        val dates = widgetCalendarDates("2027-01-01", HistoryWidgetPeriod.Week)
        assertEquals(listOf("2026-12-28", "2026-12-29", "2026-12-30", "2026-12-31", "2027-01-01", "2027-01-02", "2027-01-03"), dates)
        val fortnight = widgetCalendarDates("2027-01-01", HistoryWidgetPeriod.Fortnight)
        assertEquals("2026-12-21", fortnight.first())
        assertEquals("2027-01-03", fortnight.last())
    }

    @Test fun previousLeapMonthKeepsFebruary29() {
        val dates = widgetCalendarDates("2028-03-01", HistoryWidgetPeriod.Week)
        assertTrue("2028-02-29" in dates)
        assertEquals("2028-02-28", dates.first())
        assertEquals("2028-03-05", dates.last())
    }

    @Test fun monthStillContainsEveryDayAndTrailingPadding() {
        val dates = widgetCalendarDates("2026-03-16", HistoryWidgetPeriod.Month)
        assertEquals(42, dates.size)
        assertEquals(6, dates.takeWhile { it == null }.size)
        assertEquals((1..31).map { HistoryMonth(2026, 3).date(it) }, dates.filterNotNull())
    }

    @Test fun monthWithFourWeeksDoesNotGrowIntoSixRows() {
        assertEquals(28, widgetCalendarDates("2027-02-10", HistoryWidgetPeriod.Month).size)
    }

    @Test fun loadingIncludesPreviousMonthOnlyWhenVisibleAndNeverFutureDays() {
        val dates = historyWidgetDatesToLoad("2026-11-02")
        assertEquals("2026-10-26", dates.min())
        assertEquals("2026-11-02", dates.max())
        assertFalse("2026-10-25" in dates)
        assertFalse("2026-11-03" in dates)
        assertTrue(historyWidgetDatesToLoad("2026-10-25").all { it.startsWith("2026-10-") })
    }

    @Test fun compactViewsKeepPreviousMonthPhotosAndCountOnlyVisibleDays() {
        val photo = HistoryCalendarPhoto("previous", false)
        val october = HistoryActivityMonth(HistoryMonth(2026, 10), mapOf("2026-10-31" to HistoryActivityDay(2, photo)), false)
        val november = HistoryActivityMonth(HistoryMonth(2026, 11), mapOf("2026-11-02" to HistoryActivityDay(1, null)), false)
        val fortnight = historyWidgetCalendar("2026-11-02", HistoryWidgetPeriod.Fortnight, listOf(october, november))
        assertEquals(photo, fortnight.days["2026-10-31"]?.photo)
        assertEquals(2, fortnight.days.size)
        val month = historyWidgetCalendar("2026-11-02", HistoryWidgetPeriod.Month, listOf(october, november))
        assertEquals(setOf("2026-11-02"), month.days.keys)
    }

    @Test fun failedPreviousMonthDoesNotMarkTheCurrentMonthPartial() {
        val previous = HistoryActivityMonth(HistoryMonth(2026, 10), emptyMap(), true)
        val current = HistoryActivityMonth(HistoryMonth(2026, 11), emptyMap(), false)
        assertTrue(historyWidgetCalendar("2026-11-02", HistoryWidgetPeriod.Fortnight, listOf(previous, current)).isPartial)
        assertFalse(historyWidgetCalendar("2026-11-02", HistoryWidgetPeriod.Month, listOf(previous, current)).isPartial)
    }

    @Test fun futureEntriesNeverAppearAsCompletedActivity() {
        val current = HistoryActivityMonth(HistoryMonth(2026, 10), mapOf("2026-10-23" to HistoryActivityDay(1, null)), false)
        assertTrue(historyWidgetCalendar("2026-10-21", HistoryWidgetPeriod.Week, listOf(current)).days.isEmpty())
    }

    @Test fun supportedYearBoundsDoNotCreateInvalidMonths() {
        assertEquals(14, widgetCalendarDates("0001-01-01", HistoryWidgetPeriod.Fortnight).size)
        assertTrue(historyWidgetDatesToLoad("0001-01-01").all { it.startsWith("0001-01-") })
        assertEquals(7, widgetCalendarDates("9999-12-31", HistoryWidgetPeriod.Week).size)
    }
    @Test fun progressCountsActiveDatesAcrossTheWholeMonthEvenInWeekView() {
        val month = HistoryActivityMonth(HistoryMonth(2026, 10), mapOf(
            "2026-10-01" to HistoryActivityDay(8, null), "2026-10-25" to HistoryActivityDay(2, null),
            "2026-10-30" to HistoryActivityDay(1, null), "2026-10-02" to HistoryActivityDay(0, null)), false)
        HistoryWidgetPeriod.entries.forEach { period ->
            val calendar = historyWidgetCalendar("2026-10-25", period, listOf(month))
            assertEquals(2, calendar.activeMonthDays)
            assertEquals(31, calendar.daysInMonth)
        }
    }

    @Test fun precedingMonthPhotosDoNotInflateCurrentMonthProgress() {
        val october = HistoryActivityMonth(HistoryMonth(2026, 10), mapOf("2026-10-31" to HistoryActivityDay(4, null)), false)
        val november = HistoryActivityMonth(HistoryMonth(2026, 11), mapOf("2026-11-02" to HistoryActivityDay(2, null)), false)
        val calendar = historyWidgetCalendar("2026-11-02", HistoryWidgetPeriod.Fortnight, listOf(october, november))
        assertEquals(2, calendar.days.size)
        assertEquals(1, calendar.activeMonthDays)
        assertEquals(30, calendar.daysInMonth)
    }

    @Test fun progressDenominatorFollowsLeapYearAndEmptyMonthRules() {
        assertEquals(29, historyWidgetCalendar("2028-02-10", HistoryWidgetPeriod.Week, emptyList()).daysInMonth)
        assertEquals(28, historyWidgetCalendar("2027-02-10", HistoryWidgetPeriod.Week, emptyList()).daysInMonth)
        assertEquals(0, historyWidgetCalendar("2027-02-10", HistoryWidgetPeriod.Week, emptyList()).activeMonthDays)
    }

}
