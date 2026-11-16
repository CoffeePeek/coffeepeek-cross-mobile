package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth

/** Collapsing always returns to the current week, including adjacent-month dates. */
internal fun profileActivityDates(today: String, month: HistoryMonth, expanded: Boolean): List<String?> =
    if (expanded) historyCalendarCells(month).map { day -> day?.let(month::date) }
    else activityWeekDates(today)
