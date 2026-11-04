package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/** A home check-in brewed from the bag; [score] is 1–5. */
internal data class ShelfBrewItem(val id: String, val day: ShelfDay?, val method: String?, val doseGrams: Double?, val score: Int?, val methodCode: String? = null)

/** The open bag's home check-ins, for its card. */
internal sealed interface ShelfBagBrews {
    data object Loading : ShelfBagBrews
    data object Failed : ShelfBagBrews
    /** Newest first; [leftGrams] is the bag's weight less the brewed doses, when both are known. */
    data class Loaded(val brews: List<ShelfBrewItem>, val leftGrams: Int?) : ShelfBagBrews
}
