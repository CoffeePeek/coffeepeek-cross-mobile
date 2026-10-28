package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfMessage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfState
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlinx.coroutines.launch

/**
 * Renders the repository's shelf snapshot, which already shows moves and finishes optimistically and rolls them back
 * on failure; this model only reports the outcome. One write per bag at a time.
 */
internal class ShelfViewModel(
    private val repository: ShelfRepository,
    private val calendar: ShelfCalendar,
    private val arrivals: ShelfArrivals,
) : MviViewModel<ShelfState, ShelfAction, ShelfEvent>(ShelfState(today = calendar.today())) {
    private val busyBags = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.shelf.collect { bags ->
                updateState { copy(bags = bags.orEmpty().map(calendar::item), loaded = bags != null, today = calendar.today()) }
            }
        }
        viewModelScope.launch {
            arrivals.arriving.collect { id -> if (id != null) updateState { copy(arrivingId = id, message = ShelfMessage.Added) } }
        }
    }

    override suspend fun handleActionInternal(action: ShelfAction) {
        when (action) {
            ShelfAction.Refresh -> refresh()
            is ShelfAction.OpenBag -> updateState { copy(openBagId = action.id) }
            ShelfAction.CloseBag -> updateState { copy(openBagId = null) }
            is ShelfAction.Move -> move(action.id, action.toIndex)
            is ShelfAction.StartFinish -> updateState { copy(openBagId = null, finishingId = action.id) }
            ShelfAction.CancelFinish -> updateState { copy(finishingId = null) }
            is ShelfAction.Finish -> finish(action)
            is ShelfAction.UndoFinish -> undoFinish(action.id, action.position)
            is ShelfAction.RequestDelete -> updateState { copy(deletingId = action.id) }
            ShelfAction.CancelDelete -> updateState { copy(deletingId = null) }
            is ShelfAction.Delete -> delete(action.id)
            ShelfAction.DismissMessage -> updateState { copy(message = null) }
            is ShelfAction.ArrivalShown -> arrivalShown(action.id)
            ShelfAction.OpenArchive -> sendEvent(ShelfEvent.OpenArchive)
            ShelfAction.BrowseCatalog -> sendEvent(ShelfEvent.BrowseCatalog)
            ShelfAction.SignIn -> sendEvent(ShelfEvent.SignIn)
            ShelfAction.AddBag -> sendEvent(ShelfEvent.AddBag)
        }
    }

    private suspend fun refresh() {
        if (currentState.loading) return
        updateState { copy(loading = true, loadFailed = false) }
        val failure = repository.refresh().exceptionOrNull()
        val issue = failure?.let(::issueOf)
        updateState {
            when {
                issue == null || issue == ShelfIssue.SignedOut -> copy(loading = false)
                loaded -> copy(loading = false, message = ShelfMessage.Failed(issue))
                else -> copy(loading = false, loadFailed = true)
            }
        }
    }

    private suspend fun move(id: String, toIndex: Int) {
        val from = currentState.bags.indexOfFirst { it.id == id }
        if (from < 0 || from == toIndex) return
        write(id) { repository.move(id, toIndex).onFailure(::fail) }
    }

    private suspend fun finish(action: ShelfAction.Finish) {
        val position = currentState.bags.indexOfFirst { it.id == action.id }
        updateState { copy(finishingId = null) }
        if (position < 0) return
        write(action.id) {
            repository.finish(action.id, action.rating, action.buyAgain)
                .onSuccess { updateState { copy(message = ShelfMessage.Finished(action.id, position)) } }
                .onFailure(::fail)
        }
    }

    private fun arrivalShown(id: String) {
        arrivals.shown(id)
        updateState { copy(arrivingId = arrivingId.takeUnless { it == id }) }
    }

    private suspend fun undoFinish(id: String, position: Int) {
        updateState { copy(message = null) }
        write(id) { repository.restore(id, position).onFailure(::fail) }
    }

    private suspend fun delete(id: String) {
        updateState { copy(deletingId = null) }
        write(id) {
            repository.delete(id)
                .onSuccess { updateState { copy(openBagId = openBagId.takeUnless { it == id }, message = ShelfMessage.Deleted) } }
                .onFailure(::fail)
        }
    }

    /** Ignores a second write to the same bag while the first is still running. */
    private suspend fun write(id: String, block: suspend () -> Unit) {
        if (!busyBags.add(id)) return
        try { block() } finally { busyBags.remove(id) }
    }

    private fun fail(error: Throwable) = updateState { copy(message = ShelfMessage.Failed(issueOf(error))) }
}

internal fun issueOf(error: Throwable): ShelfIssue = (error as? ShelfFailure)?.issue ?: ShelfIssue.RequestFailed
