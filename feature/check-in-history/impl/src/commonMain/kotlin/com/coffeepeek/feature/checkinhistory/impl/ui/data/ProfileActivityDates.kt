package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth

/** Collapsing always returns to the current week, including adjacent-month dates. */
internal fun profileActivityDates(today: String, month: HistoryMonth, expanded: Boolean): List<String?> =
    if (expanded) historyCalendarCells(month).map { day -> day?.let(month::date) }
    else activityWeekDates(today)

/** The profile's collapsed week follows its selected date or the month being browsed. */
internal fun profileHistoryAnchor(today: String, month: HistoryMonth, selectedDate: String?): String =
    selectedDate ?: today.takeIf(month::containsDate) ?: month.date(1)

internal fun profileHistoryDates(today: String, month: HistoryMonth, selectedDate: String?, expanded: Boolean): List<String?> =
    profileActivityDates(profileHistoryAnchor(today, month, selectedDate), month, expanded)
