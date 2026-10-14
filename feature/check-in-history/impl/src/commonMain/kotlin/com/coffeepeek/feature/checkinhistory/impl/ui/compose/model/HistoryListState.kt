package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

internal data class HistoryListState(
    val checkIns: List<PersonalCheckIn> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasError: Boolean = false,
    val hasLoadMoreError: Boolean = false,
)
