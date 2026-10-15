package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

internal sealed interface CheckInHistoryAction {
    data object Back : CheckInHistoryAction
    data object SignIn : CheckInHistoryAction
    data class OpenShop(val checkInId: String) : CheckInHistoryAction
    data class OpenPhoto(val checkInId: String, val index: Int) : CheckInHistoryAction
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
