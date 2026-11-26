package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange

/** Calendar reads cover the displayed month and its collapsed week, including adjacent-month days. */
internal data class HistoryCalendarReadWindow(val first: HistoryMonth, val last: HistoryMonth) {
    val dates = HomeHistoryDateRange(first.date(1), last.date(last.daysInMonth))
    fun contains(date: String) = date >= dates.from && date <= dates.to
    fun period(context: HistoryCalendarContext) =
        HistoryPeriod(context.period(first).fromInclusive, context.period(last).toExclusive)
}

internal fun historyCalendarReadWindow(month: HistoryMonth, today: String, selectedDate: String?): HistoryCalendarReadWindow {
    val week = activityWeekDates(profileHistoryAnchor(today, month, selectedDate))
    val first = requireNotNull(HistoryMonth.fromDate(minOf(month.date(1), week.first())))
    val last = requireNotNull(HistoryMonth.fromDate(maxOf(month.date(month.daysInMonth), week.last())))
    return HistoryCalendarReadWindow(first, last)
}
