package com.coffeepeek.admin.ui.screen.checkins

import kotlin.test.Test
import kotlin.test.assertEquals

class CheckInCalendarTest {
    @Test
    fun weekStripCrossesMonthAndLeapDay() {
        assertEquals(listOf("2024-02-26", "2024-02-27", "2024-02-28", "2024-02-29",
            "2024-03-01", "2024-03-02", "2024-03-03"), calendarWeekDates("2024-02-29"))
        assertEquals("2024-03-01", shiftCalendarDate("2024-02-29", 1))
        assertEquals("2023-12-31", shiftCalendarDate("2024-01-01", -1))
    }

    @Test
    fun octoberReferenceStartsOnThursday() {
        assertEquals(listOf(null, null, null, 1, 2, 3, 4), calendarCells(CalendarMonth(2026, 10)).take(7))
        assertEquals("Понедельник", calendarWeekday("2026-10-12"))
    }

    @Test
    fun todaySelectionSurvivesLoadingEvenWithoutCheckInsToday() {
        assertEquals("2026-10-13", calendarSelection(CalendarMonth(2026, 10), "2026-10-13",
            "2026-10-13", setOf("2026-10-12")))
        assertEquals("2026-10-07", calendarSelection(CalendarMonth(2026, 10), "2026-10-13",
            "2026-10-07", setOf("2026-10-12")))
    }

    @Test
    fun newMonthSelectsLatestVisitAndDoesNotCarryPreviousMonthSelection() {
        assertEquals("2026-09-29", calendarSelection(CalendarMonth(2026, 9), "2026-10-13",
            "2026-10-12", setOf("2026-09-04", "2026-09-29")))
        assertEquals("2026-10-13", calendarSelection(CalendarMonth(2026, 10), "2026-10-13", null, emptySet()))
        assertEquals(null, calendarSelection(CalendarMonth(2026, 9), "2026-10-13", null, emptySet()))
    }

    @Test
    fun diaryUsesCutoutBeforeGalleryAndIgnoresBlankUrls() {
        val visit = com.coffeepeek.domain.model.CheckIn("one", "shop", "Coffee", "", "2026-10-12T09:00:00Z",
            drinkPhotoUrl = "drink.png", photoThumbnailUrls = listOf("", "thumb.jpg"), photoUrls = listOf("original.jpg"))
        assertEquals("drink.png" to true, diaryPhoto(visit))
        assertEquals("thumb.jpg" to false, diaryPhoto(visit.copy(drinkPhotoUrl = "")))
        assertEquals("original.jpg" to false, diaryPhoto(visit.copy(drinkPhotoUrl = null, photoThumbnailUrls = emptyList())))
        assertEquals(null, diaryPhoto(visit.copy(drinkPhotoUrl = null, photoThumbnailUrls = emptyList(), photoUrls = emptyList())))
    }
    @Test
    fun september2026StartsOnTuesdayAndHasThirtyDays() {
        val cells = calendarCells(CalendarMonth(2026, 9))

        assertEquals(35, cells.size)
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), cells.take(7))
        assertEquals(30, cells.filterNotNull().last())
    }

    @Test
    fun rangeEndsAtNextMonthAndLeapDayIsPresent() {
        val february = CalendarMonth(2024, 2)

        assertEquals("2024-02-01", com.coffeepeek.admin.utils.utcIsoToLocalDate(february.fromUtc))
        assertEquals("2024-03-01", com.coffeepeek.admin.utils.utcIsoToLocalDate(february.toUtc))
        assertEquals(29, calendarCells(february).filterNotNull().last())
    }

    @Test
    fun consecutiveCheckInsFormAStreak() {
        val month = CalendarMonth(2026, 9)
        val dates = setOf("2026-09-05", "2026-09-06", "2026-09-07")

        assertEquals(CheckInStreak(false, true), checkInStreak(month, 5, dates))
        assertEquals(CheckInStreak(true, true), checkInStreak(month, 6, dates))
        assertEquals(CheckInStreak(true, false), checkInStreak(month, 7, dates))
        assertEquals(CheckInStreak(false, false), checkInStreak(month, 9, dates))
    }

    @Test
    fun daysWithoutCheckInsNeverJoinAStreak() {
        val month = CalendarMonth(2026, 9)
        val dates = setOf("2026-09-05", "2026-09-06", "2026-09-08")

        assertEquals(CheckInStreak(false, false), checkInStreak(month, 4, dates))
        assertEquals(CheckInStreak(false, false), checkInStreak(month, 7, dates))
        assertEquals(CheckInStreak(false, false), checkInStreak(month, 8, dates))
    }
}
