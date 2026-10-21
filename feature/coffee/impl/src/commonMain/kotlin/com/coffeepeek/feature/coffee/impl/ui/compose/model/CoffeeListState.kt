package com.coffeepeek.feature.coffee.impl.ui.compose.model

import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterGroup
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters

internal data class CoffeeListState(
    val items: List<Coffee> = emptyList(),
    val query: String = "",
    val filters: CoffeeFilters = CoffeeFilters(),
    val filterGroups: List<CoffeeFilterGroup> = emptyList(),
    val filtersLoading: Boolean = true,
    val filtersFailed: Boolean = false,
    val filterDraft: CoffeeFilters? = null,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadFailed: Boolean = false,
    val loadMoreFailed: Boolean = false,
)
