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

    /** Compose masks account-owned content before the asynchronously reduced session action. */
    fun visibleTo(generation: Int, loggedIn: Boolean): CheckInHistoryState =
        if (sessionGeneration == generation && isLoggedIn == loggedIn) this else copy(
            sessionGeneration = generation, isLoggedIn = loggedIn,
            calendar = HistoryCalendarState(calendar.month, isLoading = loggedIn),
            list = HistoryListState(isLoading = loggedIn))
}
