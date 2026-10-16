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
    private val home = HistoryCalendarPhoto("home-cutout", true)
    private val shop = PersonalCheckIn(
        "shop", null, "Coffee", null, "Owner", "", "${day}T08:00:00Z", "${day}T08:00:00Z",
        null, listOf("original"), null, null, HistoryVisibility.Private, HistoryModeration.NotSubmitted, 0, null, 0, false,
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
        assertEquals(listOf(home, HistoryCalendarPhoto("original", false), HistoryCalendarPhoto("other", false)),
            historyCalendarPhotos(state, day))
    }

    @Test fun sourceFilterAndCutoutPriorityPreserveActualPhotoType() {
        assertEquals(listOf(HistoryCalendarPhoto("original", false)), historyCalendarPhotos(state().copy(filter = HistoryKindFilter.Shop), day))
        assertEquals(listOf(home, HistoryCalendarPhoto("other", false), HistoryCalendarPhoto("extra", false)),
            historyCalendarPhotos(state().copy(filter = HistoryKindFilter.Home), day))
    }

    @Test fun guestAndReplacedAccountCannotSeePreviousMonthPhotos() {
        assertTrue(historyCalendarPhotos(state().copy(isLoggedIn = false), day).isEmpty())
        assertTrue(historyCalendarPhotos(state().visibleTo(1, true), day).isEmpty())
        assertTrue(state().visibleTo(1, false).homeCalendarPhotos.isEmpty())
    }
}
