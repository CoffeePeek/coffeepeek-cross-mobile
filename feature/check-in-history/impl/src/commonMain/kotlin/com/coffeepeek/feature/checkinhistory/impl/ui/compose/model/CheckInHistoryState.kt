package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

internal data class CheckInHistoryState(
    val today: String,
    val isLoggedIn: Boolean,
    val sessionGeneration: Int,
    val calendar: HistoryCalendarState,
    val list: HistoryListState = HistoryListState(),
    val mode: CheckInHistoryMode = CheckInHistoryMode.Calendar,
) {
    val canGoNextMonth: Boolean get() = calendar.month.date(1).substring(0, 7) < today.substring(0, 7)
}
