package com.coffeepeek.feature.shelf.impl.ui.data

import com.coffeepeek.feature.shelf.api.ShelfBrew
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBrewItem
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

/**
 * Home check-ins are not linked to bags, so a bag's own are those made while it stood on the shelf: with its catalogue
 * coffee, or, for a coffee of its own, under the same name. A bag without either matches nothing.
 */
internal suspend fun ShelfBrewLog.forBag(item: ShelfBagItem, today: ShelfDay): ShelfBagBrews {
    val matches = item.brewMatch() ?: return ShelfBagBrews.Unmatched
    val from = item.addedOn ?: return ShelfBagBrews.Unmatched
    val to = (item.finishedOn ?: today).takeIf { it.epochDay >= from.epochDay } ?: from
    val brews = brews(from.iso, to.iso).getOrElse { error ->
        if (error is CancellationException) throw error
        return ShelfBagBrews.Failed
    }
    return shelfBagBrews(item.bag.weightGrams, brews.filter(matches))
}

private fun ShelfBagItem.brewMatch(): ((ShelfBrew) -> Boolean)? {
    bag.coffeeProductId?.let { id -> return { it.coffeeProductId == id } }
    val name = bag.name?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return { it.coffeeProductId == null && it.beanName?.trim().equals(name, ignoreCase = true) }
}

/** Newest first; what is left is the weight less every known dose, never below zero. */
internal fun shelfBagBrews(weightGrams: Int?, brews: List<ShelfBrew>): ShelfBagBrews.Loaded {
    val items = brews.sortedByDescending { it.preparedOn }.map {
        ShelfBrewItem(it.id, ShelfDay.parse(it.preparedOn), it.method?.takeIf(String::isNotBlank), it.doseGrams?.takeIf { dose -> dose > 0 },
            it.score?.takeIf { score -> score in 1..5 })
    }
    val doses = items.mapNotNull { it.doseGrams }
    val left = weightGrams?.takeIf { doses.isNotEmpty() }?.let { (it - doses.sum()).roundToInt().coerceAtLeast(0) }
    return ShelfBagBrews.Loaded(items, left)
}
