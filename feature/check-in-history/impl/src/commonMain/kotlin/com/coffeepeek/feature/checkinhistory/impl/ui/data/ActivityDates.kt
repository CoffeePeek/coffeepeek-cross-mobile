package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange

internal fun shiftActivityDate(date: String, offset: Int): String {
    var month = requireNotNull(HistoryMonth.fromDate(date))
    var day = date.takeLast(2).toInt()
    require(offset in -7..7)
    repeat(kotlin.math.abs(offset)) {
        if (offset > 0) {
            day++
            if (day > month.daysInMonth) { month = month.plusMonths(1); day = 1 }
        } else {
            day--
            if (day < 1) { month = month.plusMonths(-1); day = month.daysInMonth }
        }
    }
    return month.date(day)
}

internal fun activityWeekDates(today: String): List<String> {
    val month = requireNotNull(HistoryMonth.fromDate(today))
    val weekday = historyCalendarCells(month).indexOf(today.takeLast(2).toInt()) % 7
    val monday = shiftActivityDate(today, -weekday)
    return (0..6).map { shiftActivityDate(monday, it) }
}

internal fun activityRange(today: String, month: HistoryMonth, weekly: Boolean): HomeHistoryDateRange =
    if (weekly) activityWeekDates(today).let { HomeHistoryDateRange(it.first(), it.last()) }
    else HomeHistoryDateRange.month(month)
