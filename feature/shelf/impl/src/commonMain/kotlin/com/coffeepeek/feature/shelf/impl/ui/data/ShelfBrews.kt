package com.coffeepeek.feature.shelf.impl.ui.data

import com.coffeepeek.feature.shelf.api.ShelfBrew
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBrewItem
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

/** A bag's own home check-ins are those that name it; each check-in links its bag when it is brewed. */
internal suspend fun ShelfBrewLog.forBag(item: ShelfBagItem): ShelfBagBrews {
    val brews = brews(item.bag.id).getOrElse { error ->
        if (error is CancellationException) throw error
        return ShelfBagBrews.Failed
    }
    return shelfBagBrews(item.bag.weightGrams, brews)
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
