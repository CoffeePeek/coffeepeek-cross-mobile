package com.coffeepeek.admin.feature.coffee.ui

import com.coffeepeek.admin.analytics.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.feature.coffee.domain.Coffee
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilterGroup
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CoffeeListUiState(
    val items: List<Coffee> = emptyList(),
    val query: String = "",
    val filters: CoffeeFilters = CoffeeFilters(),
    val filterGroups: List<CoffeeFilterGroup> = emptyList(),
    val filtersLoading: Boolean = true,
    val filtersError: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val isLoadMoreError: Boolean = false,
)

internal class CoffeeListViewModel(private val repository: CoffeeRepository) : ViewModel() {
    private val _state = MutableStateFlow(CoffeeListUiState())
    val state = _state.asStateFlow()
    private val searchAnalytics = SearchTracker(Catalog.Coffees)
    private var activeSearch: SearchAttempt? = null
    private var searchJob: Job? = null

    fun openedCoffee(index: Int) = searchAnalytics.open(Entity.Coffee, index + 1)
    private var filterJob: Job? = null

    init {
        loadFilters()
        refresh()
    }

    fun loadFilters() {
        filterJob?.cancel()
        _state.update { it.copy(filtersLoading = true, filtersError = false) }
        filterJob = viewModelScope.launch {
            repository.getFilterGroups().onSuccess { groups ->
                _state.update { it.copy(filterGroups = groups, filtersLoading = false) }
            }.onFailure {
                _state.update { it.copy(filtersLoading = false, filtersError = true) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query.take(100)) }
        search(reset = true, debounce = true)
    }

    fun applyFilters(filters: CoffeeFilters) {
        val old = state.value.filters
        val keys = filters.values.filterValues { it.isNotEmpty() }.keys + "availableOnly"
        Analytics.track(AnalyticsEvent.FILTERS_APPLIED, "catalog" to "coffees", "filter_keys" to keys.sorted(),
            "changed_filter_keys" to ((old.values.keys + filters.values.keys).filter { old.values[it] != filters.values[it] } + if (old.availableOnly != filters.availableOnly) listOf("availableOnly") else emptyList()).sorted(),
            "action" to if (filters.activeCount == 0) "reset" else "apply")
        _state.update { it.copy(filters = filters) }
        search(reset = true)
    }

    fun refresh() = search(reset = true, preserveItems = true)

    fun loadMore() {
        val current = state.value
        if (!current.isLoading && !current.isRefreshing && !current.isLoadingMore && current.hasMore) search(reset = false)
    }

    private fun search(reset: Boolean, debounce: Boolean = false, preserveItems: Boolean = false) {
        searchJob?.cancel()
        _state.update {
            it.copy(
                items = if (reset && !preserveItems) emptyList() else it.items,
                page = if (reset && !preserveItems) 0 else it.page,
                hasMore = if (reset && !preserveItems) false else it.hasMore,
                isLoading = reset && (!preserveItems || it.items.isEmpty()),
                isRefreshing = reset && preserveItems && it.items.isNotEmpty(),
                isLoadingMore = !reset,
                error = null,
                isLoadMoreError = false,
            )
        }
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            val snapshot = state.value
            val page = if (reset) 1 else snapshot.page + 1
            if (reset) activeSearch = searchAnalytics.begin(snapshot.query.trim().replace(Regex("\\s+"), " ") to snapshot.filters, snapshot.query,
                (snapshot.filters.values.filterValues { it.isNotEmpty() }.keys + "availableOnly").toList())
            val search = activeSearch
            val result = repository.search(snapshot.query, snapshot.filters, page)
            coroutineContext.ensureActive()
            result.onSuccess { resultPage ->
                _state.update {
                    it.copy(
                        items = (if (reset) resultPage.items else it.items + resultPage.items).distinctBy { coffee -> coffee.slug },
                        page = resultPage.currentPage,
                        hasMore = resultPage.currentPage < resultPage.totalPages,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = null,
                    )
                }
                val count = resultPage.totalCount ?: if (resultPage.totalPages <= 1) resultPage.items.size else null
                count?.let { search?.results(it) }
            }.onFailure {
                if (reset) search?.failed(safeAnalyticsError(it))
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = "Не удалось загрузить кофе",
                        isLoadMoreError = !reset,
                    )
                }
            }
        }
    }
}
