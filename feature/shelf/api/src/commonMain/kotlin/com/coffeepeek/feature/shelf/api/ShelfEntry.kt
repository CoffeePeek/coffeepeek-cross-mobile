package com.coffeepeek.feature.shelf.api

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * The shelf screens. The application owns the tab, the root back stack, sign-in, the catalog and home check-ins.
 *
 * The functions take no default arguments: Kotlin/Native links an implementation in another module against the abstract
 * Composable's exact signature, and Compose makes defaulted parameters of the override nullable, so defaults would break iOS.
 */
interface ShelfEntry {
    /**
     * The shelf inside the Coffee tab. [header] is the app's content above the shelf (the "Моя полка / Каталог" switch);
     * a guest ([signedIn] false) sees empty shelves and an invitation to sign in. [onAddBag] opens the scan from the empty shelf
     * and [onOpenBag] the [Bag] screen of a tapped bag.
     */
    @Composable
    fun Shelf(
        signedIn: Boolean,
        onOpenArchive: () -> Unit,
        onBrowseCatalog: () -> Unit,
        onSignIn: () -> Unit,
        onAddBag: () -> Unit,
        onOpenBag: (bagId: String) -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
        header: @Composable ColumnScope.() -> Unit,
    )

    /** Finished bags as a full-screen destination; [onOpenBag] as on the shelf. */
    @Composable
    fun Archive(onBack: () -> Unit, onOpenBag: (bagId: String) -> Unit, modifier: Modifier)

    /**
     * One bag as a full-screen destination, opened from the shelf or the archive. [onOpenCoffee] opens its catalogue coffee by
     * slug. [brew] is the application's home check-in form started with the bag's coffee, shown over this screen until it calls
     * `onClose` (`saved` when a check-in was saved); without it the screen offers no "Заварить чашку".
     */
    @Composable
    fun Bag(
        bagId: String,
        onBack: () -> Unit,
        onOpenCoffee: (slug: String) -> Unit,
        modifier: Modifier,
        brew: (@Composable (coffee: ShelfBrewCoffee, onClose: (saved: Boolean) -> Unit) -> Unit)?,
    )

    /** Scan a bag and put it on the shelf, as a full-screen destination; [onSaved] after the new bag is on the shelf. */
    @Composable
    fun AddBag(onSaved: () -> Unit, onClose: () -> Unit, modifier: Modifier)
}

/**
 * The coffee a home check-in starts with: bag [shelfBagId], shown as its [name]. [slug] is the bag's catalogue coffee; the
 * check-in takes its bean from the bag either way.
 */
data class ShelfBrewCoffee(val slug: String?, val name: String?, val shelfBagId: String? = null)
