package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import com.coffeepeek.feature.shelf.api.ShelfBrewCoffee
import com.coffeepeek.feature.shelf.api.ShelfEntry

/** The application's home check-in form started with a bag's coffee, shown until it calls `onClose` (`saved` after a save). */
internal typealias ShelfBrewForm = @Composable (coffee: ShelfBrewCoffee, onClose: (saved: Boolean) -> Unit) -> Unit

/**
 * After a bag is added, the profile opens its shelf section. With [brew], a bag's screen offers
 * "Заварить чашку", which opens that form with its coffee; a platform without the migrated home check-in passes none.
 */
internal class ShelfFeatureScreenRenderer(
    private val entry: ShelfEntry,
    private val brew: ShelfBrewForm?,
) : ShelfScreenRenderer {
    @Composable
    override fun Archive() =
        entry.Archive(onBack = Navigator::popBack, onOpenBag = { Navigator.navigate(Navigator.Screen.ShelfBag(it)) }, modifier = Modifier)

    @Composable
    override fun Bag(bagId: String) = entry.Bag(bagId, onBack = Navigator::popBack,
        onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) }, modifier = Modifier, brew = brew)

    @Composable
    override fun AddBag() = entry.AddBag(
        onSaved = Navigator::returnToProfileShelf, onClose = Navigator::popBack, modifier = Modifier,
    )
}
