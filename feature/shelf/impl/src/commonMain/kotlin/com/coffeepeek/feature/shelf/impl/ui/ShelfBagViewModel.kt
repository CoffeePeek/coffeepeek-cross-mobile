package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.api.ShelfBrewCoffee
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.domain.model.ShelfBag
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagState
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlinx.coroutines.launch

/**
 * One bag's screen. It starts from the bag the shelf or the archive handed over and follows the shelf while the bag stands
 * there; without either (the app was restarted) it reads the shelf once, and an archived bag is then missing. Each write ends
 * the screen and hands its outcome back.
 */
internal class ShelfBagViewModel(
    private val bagId: String,
    private val repository: ShelfRepository,
    private val calendar: ShelfCalendar,
    private val handoff: ShelfBagHandoff,
    brews: ShelfBrewLog,
) : MviViewModel<ShelfBagState, ShelfBagAction, ShelfBagEvent>(
    ShelfBagState(today = calendar.today(), bag = handoff.opened(bagId)?.let(calendar::item)),
) {
    private val brewLoader = ShelfBrewLoader(brews, calendar)
    private var shelfIds = emptyList<String>()

    init {
        loadBrews()
        viewModelScope.launch { repository.shelf.collect(::shelfChanged) }
        if (currentState.bag == null) viewModelScope.launch {
            if (repository.refresh().isFailure && currentState.bag == null) updateState { copy(missing = true) }
        }
    }

    override suspend fun handleActionInternal(action: ShelfBagAction) {
        when (action) {
            ShelfBagAction.Back -> sendEvent(ShelfBagEvent.Back)
            ShelfBagAction.RetryBrews -> loadBrews()
            ShelfBagAction.OpenCoffee -> currentState.bag?.bag?.coffeeProductSlug?.let { sendEvent(ShelfBagEvent.OpenCoffee(it)) }
            ShelfBagAction.Brew -> currentState.bag?.bag?.let { bag ->
                updateState { copy(brewing = ShelfBrewCoffee(bag.coffeeProductSlug, bag.name?.trim()?.takeIf(String::isNotEmpty))) }
            }
            is ShelfBagAction.BrewClosed -> brewClosed(action.saved)
            ShelfBagAction.StartFinish -> updateState { copy(finishing = !archived && bag != null) }
            ShelfBagAction.CancelFinish -> updateState { copy(finishing = false) }
            is ShelfBagAction.Finish -> finish(action.rating, action.buyAgain)
            ShelfBagAction.Restore -> if (currentState.archived) write { repository.restore(bagId).map { ShelfBagOutcome.Restored(bagId) } }
            ShelfBagAction.RequestDelete -> updateState { copy(deleting = bag != null) }
            ShelfBagAction.CancelDelete -> updateState { copy(deleting = false) }
            ShelfBagAction.Delete -> delete()
            ShelfBagAction.DismissFailure -> updateState { copy(failure = null) }
        }
    }

    /** While the bag stands on the shelf the screen follows it; once it has left, the screen keeps what it last showed. */
    private fun shelfChanged(bags: List<ShelfBag>?) {
        shelfIds = bags.orEmpty().map { it.id }
        val found = bags?.firstOrNull { it.id == bagId }
        val first = currentState.bag == null
        when {
            found != null -> updateState { copy(bag = calendar.item(found), missing = false, today = calendar.today()) }
            bags != null && first -> updateState { copy(missing = true) }
        }
        if (found != null && first) loadBrews()
    }

    private fun loadBrews() {
        val item = currentState.bag ?: return
        updateState { copy(brews = ShelfBagBrews.Loading) }
        brewLoader.load(viewModelScope, item) { brews -> updateState { copy(brews = brews) } }
    }

    private fun brewClosed(saved: Boolean) {
        if (currentState.brewing == null) return
        updateState { copy(brewing = null) }
        if (saved) loadBrews()
    }

    private suspend fun finish(rating: Int?, buyAgain: Boolean?) {
        updateState { copy(finishing = false) }
        val position = shelfIds.indexOf(bagId).takeIf { it >= 0 } ?: return
        write { repository.finish(bagId, rating, buyAgain).map { ShelfBagOutcome.Finished(bagId, position) } }
    }

    private suspend fun delete() {
        val archived = currentState.archived
        updateState { copy(deleting = false) }
        write { repository.delete(bagId).map { ShelfBagOutcome.Deleted(bagId, archived) } }
    }

    /** One write at a time; on success the outcome goes back to where the bag was opened and the screen closes. */
    private suspend fun write(block: suspend () -> Result<ShelfBagOutcome>) {
        if (currentState.busy || currentState.bag == null) return
        updateState { copy(busy = true, failure = null) }
        block()
            .onSuccess { outcome ->
                handoff.report(outcome)
                sendEvent(ShelfBagEvent.Back)
            }
            .onFailure { error -> updateState { copy(failure = issueOf(error)) } }
        updateState { copy(busy = false) }
    }
}
