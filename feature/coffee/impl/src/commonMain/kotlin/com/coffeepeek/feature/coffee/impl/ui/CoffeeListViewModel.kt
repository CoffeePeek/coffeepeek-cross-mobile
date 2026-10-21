package com.coffeepeek.feature.coffee.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.coffee.api.CoffeeListAnalytics
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListAction
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListEvent
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class CoffeeListViewModel(
    private val repository: CoffeeRepository,
    private val analytics: CoffeeListAnalytics = CoffeeListAnalytics.None,
) :
    MviViewModel<CoffeeListState, CoffeeListAction, CoffeeListEvent>(CoffeeListState()) {
    private var initialized = false
    private var searchJob: Job? = null
    private var filtersJob: Job? = null
    private var searchRevision = 0L
    private var filtersRevision = 0L

    init { onAction(CoffeeListAction.Initialize) }

    override suspend fun handleActionInternal(action: CoffeeListAction) {
        when (action) {
            CoffeeListAction.Initialize -> if (!initialized) {
                initialized = true
                // Parallel independent reads, children of the lifecycle-owned initialization action.
                coroutineScope {
                    launch { loadFilters() }
                    launch { search(reset = true, preserveItems = true) }
                }
            }
            is CoffeeListAction.QueryChanged -> {
                updateState { copy(query = action.query.take(100)) }
                search(reset = true, debounce = true)
            }
            CoffeeListAction.Refresh -> search(reset = true, preserveItems = true)
            CoffeeListAction.LoadMore -> if (!currentState.isLoading && !currentState.isRefreshing &&
                !currentState.isLoadingMore && currentState.hasMore) search(reset = false)
            CoffeeListAction.RetryFilters -> loadFilters()
            CoffeeListAction.OpenFilters -> if (currentState.filterDraft == null) {
                analytics.filtersOpened()
                updateState { copy(filterDraft = filters) }
            }
            CoffeeListAction.CancelFilters -> updateState { copy(filterDraft = null) }
            CoffeeListAction.ResetFilterDraft -> if (currentState.filterDraft != null) updateState { copy(filterDraft = CoffeeFilters()) }
            is CoffeeListAction.FilterToggled -> if (currentState.filterGroups.any { group ->
                group.code == action.group && group.options.any { it.code == action.option }
            }) updateState { copy(filterDraft = filterDraft?.toggle(action.group, action.option)) }
            is CoffeeListAction.AvailableOnlyChanged -> updateState { copy(filterDraft = filterDraft?.copy(availableOnly = action.selected)) }
            CoffeeListAction.ApplyFilters -> currentState.filterDraft?.let { draft ->
                analytics.filtersApplied(draft.analyticsKeys().sorted(), changedKeys(currentState.filters, draft), draft.activeCount == 0)
                updateState { copy(filters = draft, filterDraft = null) }
                search(reset = true)
            }
            is CoffeeListAction.OpenCoffee -> if (action.slug.isNotBlank() && currentState.items.any { it.slug == action.slug }) {
                analytics.opened(currentState.items.indexOfFirst { it.slug == action.slug } + 1)
                sendEvent(CoffeeListEvent.OpenCoffee(action.slug))
            }
        }
    }

    private suspend fun search(reset: Boolean, debounce: Boolean = false, preserveItems: Boolean = false) {
        searchJob?.cancel()
        val job = currentCoroutineContext().job
        val revision = ++searchRevision
        searchJob = job
        val snapshot = currentState
        val page = if (reset) 1 else snapshot.page + 1
        updateState { copy(
            items = if (reset && !preserveItems) emptyList() else items,
            page = if (reset && !preserveItems) 0 else this.page,
            hasMore = if (reset && !preserveItems) false else hasMore,
            isLoading = reset && (!preserveItems || items.isEmpty()),
            isRefreshing = reset && preserveItems && items.isNotEmpty(),
            isLoadingMore = !reset,
            loadFailed = false, loadMoreFailed = false,
        ) }
        try {
            if (debounce) delay(300)
            if (reset) analytics.searchStarted(snapshot.fingerprint(), snapshot.query, snapshot.filters.analyticsKeys())
            val pageResult = repository.search(snapshot.query, snapshot.filters, page).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (searchJob !== job || revision != searchRevision) return
            if (reset) (pageResult.totalCount ?: pageResult.items.size.takeIf { pageResult.totalPages <= 1 })?.let(analytics::resultsViewed)
            updateState { copy(
                items = (if (reset) pageResult.items else items + pageResult.items).distinctBy { it.slug },
                page = pageResult.currentPage,
                hasMore = pageResult.currentPage < pageResult.totalPages,
            ) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            if (searchJob === job && revision == searchRevision && job.isActive) {
                if (reset) analytics.searchFailed(failure)
                updateState { copy(loadFailed = true, loadMoreFailed = !reset) }
            }
        } finally {
            if (searchJob === job) {
                searchJob = null
                updateState { copy(isLoading = false, isRefreshing = false, isLoadingMore = false) }
            }
        }
    }

    private suspend fun loadFilters() {
        filtersJob?.cancel()
        val job = currentCoroutineContext().job
        val revision = ++filtersRevision
        filtersJob = job
        updateState { copy(filtersLoading = true, filtersFailed = false) }
        try {
            val groups = repository.getFilterGroups().getOrThrow()
            currentCoroutineContext().ensureActive()
            if (filtersJob === job && revision == filtersRevision) updateState { copy(filterGroups = groups) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (filtersJob === job && revision == filtersRevision && job.isActive) updateState { copy(filtersFailed = true) }
        } finally {
            if (filtersJob === job) { filtersJob = null; updateState { copy(filtersLoading = false) } }
        }
    }

    override fun onCleared() {
        searchRevision++; filtersRevision++
        searchJob?.cancel(); filtersJob?.cancel()
        super.onCleared()
    }
}

private fun CoffeeFilters.analyticsKeys(): List<String> =
    values.filterValues { it.isNotEmpty() }.keys.toList() + if (availableOnly) listOf("availableOnly") else emptyList()

private fun changedKeys(old: CoffeeFilters, new: CoffeeFilters): List<String> =
    ((old.values.keys + new.values.keys).filter { old.values[it] != new.values[it] } +
        if (old.availableOnly != new.availableOnly) listOf("availableOnly") else emptyList()).sorted()

/** Canonical identity of the criteria: whitespace-normalized query and sorted selections. */
private fun CoffeeListState.fingerprint(): String =
    query.trim().replace(Regex("\\s+"), " ") + "|" + filters.values.filterValues { it.isNotEmpty() }.toSortedMap()
        .entries.joinToString(";") { (group, options) -> group + "=" + options.sorted().joinToString(",") } +
        "|" + filters.availableOnly
