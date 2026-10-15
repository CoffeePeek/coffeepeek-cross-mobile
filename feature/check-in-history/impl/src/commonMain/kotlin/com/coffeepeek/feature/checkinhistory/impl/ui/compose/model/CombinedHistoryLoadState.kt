package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

/** Feature-local request state; failures never contain backend text or Throwable objects. */
internal data class CombinedHistoryLoadState<T>(
    val content: T,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

internal data class CombinedHistoryPageState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasError: Boolean = false,
    val hasLoadMoreError: Boolean = false,
)
