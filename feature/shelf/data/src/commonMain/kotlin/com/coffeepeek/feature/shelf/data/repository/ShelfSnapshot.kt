package com.coffeepeek.feature.shelf.data.repository

import com.coffeepeek.feature.shelf.domain.model.ShelfBag

/**
 * [confirmed] is the server's last answer; [shown] adds the optimistic changes still in flight ([pending]).
 * Once nothing is pending, [shown] equals [confirmed] again.
 */
internal data class ShelfSnapshot(val owner: String, val confirmed: List<ShelfBag>, val shown: List<ShelfBag>, val pending: Int = 0) {
    /** Applies a server-confirmed [change] to both lists. */
    fun confirm(change: (List<ShelfBag>) -> List<ShelfBag>): ShelfSnapshot {
        val next = change(confirmed).renumbered()
        return copy(confirmed = next, shown = if (pending == 0) next else change(shown).renumbered())
    }

    fun optimistic(change: (List<ShelfBag>) -> List<ShelfBag>) = copy(shown = change(shown).renumbered(), pending = pending + 1)

    /** Ends one optimistic change: [confirmed] on success, or a rollback of every unconfirmed change on failure. */
    fun settle(change: ((List<ShelfBag>) -> List<ShelfBag>)?): ShelfSnapshot {
        val next = change?.let { confirmed.let(it).renumbered() } ?: confirmed
        val left = pending - 1
        return copy(confirmed = next, shown = if (left == 0 || change == null) next else shown, pending = left)
    }

    /** The order to send: the shown order of confirmed bags, then confirmed bags hidden by a pending finish. */
    fun orderToSend(): List<String> {
        val confirmedIds = confirmed.map(ShelfBag::id)
        val shownIds = shown.map(ShelfBag::id).filter(confirmedIds::contains)
        return shownIds + confirmedIds.filterNot(shownIds::contains)
    }
}

internal fun List<ShelfBag>.renumbered() = mapIndexed { index, bag -> if (bag.position == index) bag else bag.copy(position = index) }

internal fun List<ShelfBag>.moved(id: String, toIndex: Int): List<ShelfBag> {
    val bag = firstOrNull { it.id == id } ?: return this
    val rest = filterNot { it.id == id }
    return rest.toMutableList().apply { add(toIndex.coerceIn(0, rest.size), bag) }
}
