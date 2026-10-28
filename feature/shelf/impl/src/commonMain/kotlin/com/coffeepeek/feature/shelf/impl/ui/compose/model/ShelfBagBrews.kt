package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/** A home check-in with the bag's coffee; [score] is 1–5. */
internal data class ShelfBrewItem(val id: String, val day: ShelfDay?, val method: String?, val doseGrams: Double?, val score: Int?)

/** The open bag's home check-ins, for its card. */
internal sealed interface ShelfBagBrews {
    /** The bag names no coffee, so no check-in can be told apart as its own. */
    data object Unmatched : ShelfBagBrews
    data object Loading : ShelfBagBrews
    data object Failed : ShelfBagBrews
    /** Newest first; [leftGrams] is the bag's weight less the brewed doses, when both are known. */
    data class Loaded(val brews: List<ShelfBrewItem>, val leftGrams: Int?) : ShelfBagBrews
}
