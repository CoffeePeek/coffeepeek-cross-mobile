package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.feature.checkinhistory.impl.ui.data.activityWeekDates

internal data class ProfileActivityState(val calendar: CombinedHistoryState) {
    val loading get() = calendar.shopCalendar.isLoading || calendar.homeCalendar.isLoading
    val failed get() = calendar.isCalendarUnavailable || calendar.shopCalendar.hasError || calendar.homeCalendar.hasError
    val weekDates get() = activityWeekDates(calendar.today)
    /** Production groups visits first, then home brews, ascending within each source. */
    val selectedRows: List<HistoryTimelineItem> get() {
        val day = calendar.selectedDate ?: return emptyList()
        val shops = calendar.shopCalendar.content[day].orEmpty()
            .sortedBy { parseServerInstant(it.timestamp) }
        val homes = calendar.homes.items.filter { it.date == day }.sortedBy { parseServerInstant(it.timestamp) }
        return shops + homes
    }
}

internal sealed interface ProfileActivityAction {
    data class ChangeMonth(val offset: Int) : ProfileActivityAction
    data class SelectDate(val date: String) : ProfileActivityAction
    data class OpenRow(val key: String, val owner: Any = Unit) : ProfileActivityAction
    data object Today : ProfileActivityAction
    data object Retry : ProfileActivityAction
    data object Resume : ProfileActivityAction
    data class OpenWeek(val owner: Any = Unit) : ProfileActivityAction
}

internal sealed interface ProfileActivityEvent {
    data class OpenShop(val slug: String) : ProfileActivityEvent
    data object OpenHistory : ProfileActivityEvent
    data object OpenWeek : ProfileActivityEvent
}

/** A buffered effect from a closed sheet must not navigate when the same model is shown again. */
internal data class ProfileActivityEventEnvelope(val event: ProfileActivityEvent, val owner: Any) {
    fun forOwner(current: Any): ProfileActivityEvent? = event.takeIf { owner === current }
}
