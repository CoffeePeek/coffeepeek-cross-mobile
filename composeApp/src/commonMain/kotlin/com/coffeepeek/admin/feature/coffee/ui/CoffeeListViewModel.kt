package com.coffeepeek.admin.feature.coffee.ui

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
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

internal class CoffeeListViewModel(private val repository: CoffeeRepository) : ViewModel() {
    private val _state = MutableStateFlow(CoffeeListUiState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
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
        _state.update { it.copy(filters = filters) }
        refresh()
    }

    fun refresh() = search(reset = true)

    fun loadMore() {
        val current = state.value
        if (!current.isLoading && !current.isLoadingMore && current.hasMore) search(reset = false)
    }

    private fun search(reset: Boolean, debounce: Boolean = false) {
        searchJob?.cancel()
        _state.update {
            it.copy(
                items = if (reset) emptyList() else it.items,
                isLoading = reset,
                isLoadingMore = !reset,
                error = null,
            )
        }
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            val snapshot = state.value
            val page = if (reset) 1 else snapshot.page + 1
            val result = repository.search(snapshot.query, snapshot.filters, page)
            coroutineContext.ensureActive()
            result.onSuccess { resultPage ->
                _state.update {
                    it.copy(
                        items = (if (reset) resultPage.items else it.items + resultPage.items).distinctBy { coffee -> coffee.slug },
                        page = resultPage.currentPage,
                        hasMore = resultPage.currentPage < resultPage.totalPages,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                    )
                }
            }.onFailure {
                _state.update { it.copy(isLoading = false, isLoadingMore = false, error = "Не удалось загрузить кофе") }
            }
        }
    }
}
