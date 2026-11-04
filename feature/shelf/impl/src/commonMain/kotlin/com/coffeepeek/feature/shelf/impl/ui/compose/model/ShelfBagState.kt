package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.api.ShelfBrewCoffee
import com.coffeepeek.feature.shelf.domain.model.ShelfBagStatus
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/**
 * A bag's screen. [bag] is null until it is found and [missing] when it cannot be; [finishing] is the "Закончилась" summary,
 * [deleting] the delete confirmation and [brewing] the home check-in form over the screen. [busy] while a write runs.
 */
internal data class ShelfBagState(
    val today: ShelfDay,
    val bag: ShelfBagItem? = null,
    val missing: Boolean = false,
    val brews: ShelfBagBrews? = null,
    val finishing: Boolean = false,
    val deleting: Boolean = false,
    val brewing: ShelfBrewCoffee? = null,
    val busy: Boolean = false,
    val failure: ShelfIssue? = null,
) {
    val archived: Boolean get() = bag?.bag?.status == ShelfBagStatus.Finished
}
