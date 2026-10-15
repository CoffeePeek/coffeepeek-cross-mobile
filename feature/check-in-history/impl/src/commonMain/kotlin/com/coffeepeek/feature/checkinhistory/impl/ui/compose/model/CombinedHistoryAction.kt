package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

internal sealed interface CombinedHistoryAction {
    data class SelectFilter(val filter: HistoryKindFilter) : CombinedHistoryAction
    data class SelectDate(val date: String?) : CombinedHistoryAction
    data class ChangeMonth(val offset: Int) : CombinedHistoryAction
    data object Today : CombinedHistoryAction
    data object Refresh : CombinedHistoryAction
    data object Resume : CombinedHistoryAction
    data object LoadMore : CombinedHistoryAction
    data class RetryCalendar(val source: HistorySource) : CombinedHistoryAction
    data class RetryTimeline(val source: HistorySource) : CombinedHistoryAction
    data class RetryLoadMore(val source: HistorySource) : CombinedHistoryAction
    data class ContentChanged(val generation: Int) : CombinedHistoryAction
    /** Account replacement only. Token renewal retains the current generation. */
    data class SessionChanged(val generation: Int, val isLoggedIn: Boolean) : CombinedHistoryAction
}
