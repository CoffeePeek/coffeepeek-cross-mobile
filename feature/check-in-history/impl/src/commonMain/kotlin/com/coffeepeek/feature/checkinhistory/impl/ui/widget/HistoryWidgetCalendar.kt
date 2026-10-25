package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryActivityDay
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryActivityMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarCells

internal enum class HistoryWidgetPeriod(val minimumHeightDp: Float) {
    Week(80f), Fortnight(160f), Month(250f),
}

internal data class HistoryWidgetCalendar(
    val dates: List<String?>,
    val days: Map<String, HistoryActivityDay>,
    val isPartial: Boolean,
)

/** The current month plus any preceding days shown by the two-week layout; never request future months. */
fun historyWidgetDatesToLoad(today: String): Set<String> = HistoryWidgetPeriod.entries
    .flatMap { widgetCalendarDates(today, it) }.filterNotNull().filter { it <= today }.toSet()

internal fun historyWidgetCalendar(today: String, period: HistoryWidgetPeriod,
    activity: List<HistoryActivityMonth>): HistoryWidgetCalendar {
    val dates = widgetCalendarDates(today, period)
    val visibleDates = dates.filterNotNull().toSet()
    val months = visibleDates.mapNotNull(HistoryMonth::fromDate).toSet()
    val sources = activity.filter { it.month in months }
    return HistoryWidgetCalendar(dates, sources.flatMap { it.days.entries }
        .filter { it.key in visibleDates && it.key <= today }.associate { it.key to it.value }, sources.any { it.isPartial })
}

/** Monday-first week; fortnight is the previous and current weeks, keeping recent activity visible. */
internal fun widgetCalendarDates(today: String, period: HistoryWidgetPeriod): List<String?> {
    val month = requireNotNull(HistoryMonth.fromDate(today))
    val day = today.takeLast(2).toInt()
    val monthCells = historyCalendarCells(month)
    if (period == HistoryWidgetPeriod.Month) return monthCells.map { it?.let(month::date) }
    val weekday = monthCells.indexOf(day) % 7
    val start = -weekday - if (period == HistoryWidgetPeriod.Fortnight) 7 else 0
    val count = if (period == HistoryWidgetPeriod.Fortnight) 14 else 7
    return List(count) { shiftWidgetDate(month, day + start + it) }
}

private fun shiftWidgetDate(month: HistoryMonth, day: Int): String? = when {
    day < 1 -> if (month == HistoryMonth(1, 1)) null else month.plusMonths(-1).let { it.date(it.daysInMonth + day) }
    day > month.daysInMonth -> if (month == HistoryMonth(9999, 12)) null else month.plusMonths(1).date(day - month.daysInMonth)
    else -> month.date(day)
}
