package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.domain.model.ShelfBag
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/** A bag with its server instants already turned into the user's calendar days. */
internal data class ShelfBagItem(val bag: ShelfBag, val addedOn: ShelfDay?, val finishedOn: ShelfDay?, val roastedOn: ShelfDay?) {
    val id: String get() = bag.id

    /** Until it was finished, or until [today] while it is still on the shelf. */
    fun daysOnShelf(today: ShelfDay): Int? {
        val start = addedOn ?: return null
        return ((finishedOn ?: today).epochDay - start.epochDay).toInt().coerceAtLeast(0)
    }

    fun daysSinceRoast(today: ShelfDay): Int? = roastedOn?.let { (today.epochDay - it.epochDay).toInt().coerceAtLeast(0) }
}

/** The line above the shelf: how many bags, roughly how much coffee and from how many roasters. */
internal data class ShelfSummary(val bags: Int, val grams: Int?, val roasters: Int)

internal fun List<ShelfBagItem>.summary() = ShelfSummary(
    bags = size,
    grams = mapNotNull { it.bag.weightGrams }.takeIf { it.isNotEmpty() }?.sum(),
    roasters = mapNotNull { it.bag.roasterName?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }.distinct().size,
)
