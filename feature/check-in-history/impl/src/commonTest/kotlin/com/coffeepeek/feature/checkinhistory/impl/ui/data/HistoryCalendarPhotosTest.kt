package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryLoadState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryKindFilter
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryTimelineItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryCalendarPhotosTest {
    private val day = "2026-10-16"
    private val home = HistoryCalendarPhoto("home-photo", false, "${day}T09:00:00Z")
    private val original = HistoryCalendarPhoto("original", false, "${day}T08:00:00Z")
    private val shop = PersonalCheckIn(
        "shop", null, "Coffee", null, "Owner", "", "${day}T08:00:00Z", "${day}T08:00:00Z",
        null, listOf("original"), null, HistoryVisibility.Private, HistoryModeration.NotSubmitted, 0, null, 0, false,
    )
    private fun state() = CombinedHistoryState(
        today = day, month = HistoryMonth(2026, 10), isLoggedIn = true, sessionGeneration = 0,
        selectedDate = "2026-10-01",
        shopCalendar = CombinedHistoryLoadState(mapOf(day to listOf(HistoryTimelineItem.Shop(shop, day)))),
        homeCalendarPhotos = mapOf(day to listOf(home, home, HistoryCalendarPhoto("other", false), HistoryCalendarPhoto("extra", false))),
    )

    @Test fun monthPhotosAreIndependentOfSelectedDayAndTimelinePages() {
        val state = state()
        assertTrue(state.timeline.isEmpty())
        assertEquals(listOf(home, original, HistoryCalendarPhoto("other", false)),
            historyCalendarPhotos(state, day))
    }

    @Test fun sourceFilterPreservesActualPhotoType() {
        assertEquals(listOf(original), historyCalendarPhotos(state().copy(filter = HistoryKindFilter.Shop), day))
        assertEquals(listOf(home, HistoryCalendarPhoto("other", false), HistoryCalendarPhoto("extra", false)),
            historyCalendarPhotos(state().copy(filter = HistoryKindFilter.Home), day))
    }

    @Test fun newestPhotographedVisitWinsAcrossSources() {
        val newer = shop.copy(id = "newer", visitedAt = "${day}T12:00:00Z", photoUrls = listOf("newer-original"))
        val noPhoto = shop.copy(id = "latest-without-photo", visitedAt = "${day}T14:00:00Z", photoUrls = emptyList())
        val source = state().copy(shopCalendar = CombinedHistoryLoadState(mapOf(day to listOf(
            HistoryTimelineItem.Shop(shop, day), HistoryTimelineItem.Shop(noPhoto, day), HistoryTimelineItem.Shop(newer, day)))))
        assertEquals("newer-original", historyCalendarPhotos(source, day).first().url)
        val laterHome = home.copy(occurredAt = "${day}T13:00:00Z")
        assertEquals(laterHome, historyCalendarPhotos(source.copy(homeCalendarPhotos = mapOf(day to listOf(laterHome))), day).first())
    }

    @Test fun orderingUsesInstantsNotIsoText() {
        val newer = shop.copy(visitedAt = "${day}T09:30:00+03:00")
        val earlierHome = home.copy(occurredAt = "${day}T06:00:00Z")
        val source = state().copy(shopCalendar = CombinedHistoryLoadState(mapOf(day to listOf(HistoryTimelineItem.Shop(newer, day)))),
            homeCalendarPhotos = mapOf(day to listOf(earlierHome)))
        assertEquals("original", historyCalendarPhotos(source, day).first().url)
        assertEquals("home-photo", historyCalendarPhotos(source.copy(
            homeCalendarPhotos = mapOf(day to listOf(home.copy(occurredAt = "${day}T07:00:00Z")))), day).first().url)
    }

    @Test fun blankVisitTimeFallsBackToCreationAndMalformedOrMissingTimesDoNotBreakTheCalendar() {
        val undated = shop.copy(visitedAt = "", createdAt = "${day}T10:00:00Z")
        val source = state().copy(shopCalendar = CombinedHistoryLoadState(mapOf(day to listOf(HistoryTimelineItem.Shop(undated, day)))),
            homeCalendarPhotos = mapOf(day to listOf(home.copy(occurredAt = "malformed"), HistoryCalendarPhoto("unknown", false))))
        val photos = historyCalendarPhotos(source, day)
        assertEquals(listOf("original", "home-photo", "unknown"), photos.map { it.url })
        assertEquals("${day}T10:00:00Z", photos.first().occurredAt)
    }

    @Test fun guestAndReplacedAccountCannotSeePreviousMonthPhotos() {
        assertTrue(historyCalendarPhotos(state().copy(isLoggedIn = false), day).isEmpty())
        assertTrue(historyCalendarPhotos(state().visibleTo(1, true), day).isEmpty())
        assertTrue(state().visibleTo(1, false).homeCalendarPhotos.isEmpty())
    }

    @Test fun calendarUsesThumbVariantsAndDoesNotDownloadUnavailableOriginals() {
        fun photos(entry: PersonalCheckIn) = historyCalendarPhotos(state().copy(filter = HistoryKindFilter.Shop,
            shopCalendar = CombinedHistoryLoadState(mapOf(day to listOf(HistoryTimelineItem.Shop(entry, day))))), day)
        val entry = shop.copy(photoCalendarUrls = listOf("calendar-thumb"))
        assertEquals(listOf("calendar-thumb"), photos(entry).map { it.url })
        assertTrue(photos(entry.copy(photoCalendarUrls = listOf(""))).isEmpty())
    }
}
