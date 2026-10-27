package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveMessage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveState
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val SHELF_ARCHIVE_PAGE_SIZE = 20
internal const val SHELF_ARCHIVE_QUERY_LIMIT = 100

/** Pages of the archive for the current query; a new query starts over and drops answers to the old one. */
internal class ShelfArchiveViewModel(
    private val repository: ShelfRepository,
    private val calendar: ShelfCalendar,
    private val debounceMillis: Long = 300,
) : MviViewModel<ShelfArchiveState, ShelfArchiveAction, ShelfArchiveEvent>(ShelfArchiveState(today = calendar.today())) {
    private var search: Job? = null
    private var generation = 0
    private val busyBags = mutableSetOf<String>()

    init {
        reload(debounce = false)
    }

    override suspend fun handleActionInternal(action: ShelfArchiveAction) {
        when (action) {
            is ShelfArchiveAction.QueryChanged -> queryChanged(action.query)
            ShelfArchiveAction.Retry -> if (currentState.loadMoreFailed) loadMore() else reload(debounce = false)
            ShelfArchiveAction.LoadMore -> loadMore()
            is ShelfArchiveAction.OpenBag -> updateState { copy(openBagId = action.id) }
            ShelfArchiveAction.CloseBag -> updateState { copy(openBagId = null) }
            is ShelfArchiveAction.Restore -> restore(action.id)
            is ShelfArchiveAction.RequestDelete -> updateState { copy(deletingId = action.id) }
            ShelfArchiveAction.CancelDelete -> updateState { copy(deletingId = null) }
            is ShelfArchiveAction.Delete -> delete(action.id)
            ShelfArchiveAction.DismissMessage -> updateState { copy(message = null) }
            ShelfArchiveAction.Back -> sendEvent(ShelfArchiveEvent.Back)
        }
    }

    private fun queryChanged(query: String) {
        val limited = query.take(SHELF_ARCHIVE_QUERY_LIMIT)
        if (limited == currentState.query) return
        updateState { copy(query = limited) }
        reload(debounce = limited.isNotBlank())
    }

    private fun reload(debounce: Boolean) {
        search?.cancel()
        val request = ++generation
        updateState { copy(loading = true, loadingMore = false, loadFailed = false, loadMoreFailed = false) }
        search = viewModelScope.launch {
            if (debounce) delay(debounceMillis)
            val result = repository.archive(1, SHELF_ARCHIVE_PAGE_SIZE, currentState.query)
            if (request != generation) return@launch
            result
                .onSuccess { page -> updateState { copy(items = page.items.map(calendar::item), page = 1, hasMore = page.hasMore, loading = false) } }
                .onFailure { updateState { copy(items = emptyList(), page = 0, hasMore = false, loading = false, loadFailed = true) } }
        }
    }

    private suspend fun loadMore() {
        val state = currentState
        if (state.loading || state.loadingMore || !state.hasMore) return
        val request = generation
        updateState { copy(loadingMore = true, loadMoreFailed = false) }
        val result = repository.archive(state.page + 1, SHELF_ARCHIVE_PAGE_SIZE, state.query)
        if (request != generation) return
        result.onSuccess { page ->
            updateState {
                val known = items.mapTo(HashSet()) { it.id }
                val added = page.items.filterNot { it.id in known }.map(calendar::item)
                copy(items = items + added, page = page.page, hasMore = page.hasMore, loadingMore = false)
            }
        }.onFailure { updateState { copy(loadingMore = false, loadMoreFailed = true) } }
    }

    private suspend fun restore(id: String) {
        updateState { copy(openBagId = null) }
        write(id) { repository.restore(id).onSuccess { removed(id, ShelfArchiveMessage.Restored) }.onFailure(::fail) }
    }

    private suspend fun delete(id: String) {
        updateState { copy(deletingId = null) }
        write(id) { repository.delete(id).onSuccess { removed(id, ShelfArchiveMessage.Deleted) }.onFailure(::fail) }
    }

    private fun removed(id: String, message: ShelfArchiveMessage) = updateState {
        copy(items = items.filterNot { it.id == id }, openBagId = openBagId.takeUnless { it == id }, message = message)
    }

    private suspend fun write(id: String, block: suspend () -> Unit) {
        if (!busyBags.add(id)) return
        try { block() } finally { busyBags.remove(id) }
    }

    private fun fail(error: Throwable) = updateState { copy(message = ShelfArchiveMessage.Failed(issueOf(error))) }
}
