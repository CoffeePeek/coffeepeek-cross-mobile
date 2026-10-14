package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

internal sealed interface CheckInHistoryAction {
    data class SelectMode(val mode: CheckInHistoryMode) : CheckInHistoryAction
    data class ChangeMonth(val offset: Int) : CheckInHistoryAction
    data class SelectDate(val date: String) : CheckInHistoryAction
    data object Refresh : CheckInHistoryAction
    data object Retry : CheckInHistoryAction
    data object LoadMore : CheckInHistoryAction
    data object RetryLoadMore : CheckInHistoryAction
    data object Resume : CheckInHistoryAction
    data class ContentChanged(val generation: Int) : CheckInHistoryAction
    data class SessionChanged(val generation: Int, val isLoggedIn: Boolean) : CheckInHistoryAction
}
