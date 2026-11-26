package com.coffeepeek.feature.checkinhistory.impl

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarPhotos
import com.coffeepeek.feature.checkinhistory.impl.ui.data.profileHistoryDates

class ProfileActivitySnapshot internal constructor(state: CombinedHistoryState, expanded: Boolean) {
    val year = state.month.year
    val month = state.month.month
    val today = state.today
    val canGoNext = state.canGoNextMonth
    val canGoPrevious = state.month != HistoryMonth(1, 1)
    val loading = state.shopCalendar.isLoading || state.homeCalendar.isLoading
    val failed = state.isCalendarUnavailable || state.shopCalendar.hasError || state.homeCalendar.hasError
    val selected = state.selectedDate
    val days = profileHistoryDates(today, state.month, state.selectedDate, expanded).map { date ->
        ProfileActivityDay(date, state)
    }
}

class ProfileActivityDay internal constructor(val date: String?, calendar: CombinedHistoryState) {
    val homeCount = date?.let { calendar.homeCalendar.content[it] } ?: 0
    val shopCount = date?.let { calendar.shopCalendar.content[it].orEmpty().size.toLong() } ?: 0L
    val photo = date?.let { historyCalendarPhotos(calendar, it, maxPhotos = 1).firstOrNull()?.url }
    val selected = date != null && calendar.selectedDate == date
    val today = date == calendar.today
    val enabled = date != null && date <= calendar.today
}
