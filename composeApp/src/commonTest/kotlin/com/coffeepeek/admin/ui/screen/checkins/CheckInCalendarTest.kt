package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInPhoto
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckInCalendarTest {
    @Test
    fun calendarKeepsAllUniqueHomeAndCafePhotosInOrder() {
        val visits = listOf(
            CheckIn("original", "shop", "Coffee", "", "2026-10-12T09:00:00Z", photoUrls = listOf("original.jpg")),
            CheckIn("cafe", "shop", "Coffee", "", "2026-10-12T10:00:00Z", photoUrls = listOf("cafe.jpg")),
        )
        val photos = calendarPhotos(visits, listOf(
            CalendarPhoto("home.jpg"), CalendarPhoto("fourth.jpg"), CalendarPhoto("fifth.jpg"),
        ))
        assertEquals(listOf(
            CalendarPhoto("original.jpg"), CalendarPhoto("cafe.jpg"), CalendarPhoto("home.jpg"),
            CalendarPhoto("fourth.jpg"), CalendarPhoto("fifth.jpg"),
        ), photos)
        assertEquals(listOf(CalendarPhoto("cafe.jpg")),
            calendarPhotos(visits.takeLast(1), listOf(CalendarPhoto("cafe.jpg"))))
    }

    @Test
    fun homeCalendarKeepsPhotosGroupedByPreparedDayAndIgnoresBlankUrls() {
        fun entry(id: String, day: String, photos: List<HomeCheckInPhoto>) = HomeCheckIn(
            id, "${day}T09:00:00Z", day, "${day}T09:00:00Z", null, null, null, null,
            null, null, null, null, null, null, photos,
        )
        assertEquals(mapOf(
            "2026-10-12" to listOf(CalendarPhoto("first.jpg"), CalendarPhoto("home.jpg")),
            "2026-10-13" to emptyList(),
        ), homeCalendarPhotos(listOf(
            entry("first", "2026-10-12", listOf(HomeCheckInPhoto("original", "first.jpg"))),
            entry("original", "2026-10-12", listOf(HomeCheckInPhoto("blank", ""), HomeCheckInPhoto("photo", "home.jpg"))),
            entry("empty", "2026-10-13", emptyList()),
        )))
    }

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
        assertEquals("Понедельник", calendarWeekday("2026-10-12", listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")))
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
    fun diaryUsesPhotoVariantsAndIgnoresBlankUrls() {
        val visit = com.coffeepeek.domain.model.CheckIn("one", "shop", "Coffee", "", "2026-10-12T09:00:00Z",
            photoThumbnailUrls = listOf("", "thumb.jpg"), photoUrls = listOf("original.jpg"))
        assertEquals("calendar.jpg", diaryPhoto(visit.copy(photoCalendarUrls = listOf("", "calendar.jpg"))))
        assertEquals("thumb.jpg", diaryPhoto(visit))
        assertEquals("original.jpg", diaryPhoto(visit.copy(photoThumbnailUrls = emptyList())))
        assertEquals(null, diaryPhoto(visit.copy(photoThumbnailUrls = emptyList(), photoUrls = emptyList())))
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
    @Test
    fun weekdayUsesPreparedBelarusianNames() {
        assertEquals("Панядзелак", calendarWeekday("2026-10-12", listOf("Панядзелак", "Аўторак", "Серада", "Чацвер", "Пятніца", "Субота", "Нядзеля")))
    }
}
