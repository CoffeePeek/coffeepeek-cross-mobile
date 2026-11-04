package com.coffeepeek.feature.shelf.impl.ui

import com.coffeepeek.feature.shelf.domain.model.ShelfBag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What became of a bag on its own screen; the screen it was opened from reports it. */
internal sealed interface ShelfBagOutcome {
    val bagId: String

    /** Finished from the shelf's [position], which an undo puts it back at. */
    data class Finished(override val bagId: String, val position: Int) : ShelfBagOutcome
    data class Restored(override val bagId: String) : ShelfBagOutcome
    data class Deleted(override val bagId: String, val archived: Boolean) : ShelfBagOutcome
}

/**
 * Between the shelf or the archive and a bag's screen. The bag being opened is handed over as it was shown, since an archived
 * bag cannot be read on its own; what became of it comes back once, for the shelf or the archive to report.
 */
internal class ShelfBagHandoff {
    private val opened = mutableMapOf<String, ShelfBag>()
    private val latest = MutableStateFlow<ShelfBagOutcome?>(null)
    val outcome: StateFlow<ShelfBagOutcome?> = latest.asStateFlow()

    fun open(bag: ShelfBag) {
        opened[bag.id] = bag
    }

    fun opened(id: String): ShelfBag? = opened[id]

    fun report(outcome: ShelfBagOutcome) {
        opened.remove(outcome.bagId)
        latest.value = outcome
    }

    fun consumed(outcome: ShelfBagOutcome) {
        latest.compareAndSet(outcome, null)
    }
}
