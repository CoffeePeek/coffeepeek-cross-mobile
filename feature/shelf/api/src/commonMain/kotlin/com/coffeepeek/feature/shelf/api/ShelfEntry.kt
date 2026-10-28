package com.coffeepeek.feature.shelf.api

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The shelf screens. The application owns the tab, the root back stack, sign-in, the catalog and home check-ins. */
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
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
        header: @Composable ColumnScope.() -> Unit = {},
    )

    /** Finished bags as a full-screen destination; [onOpenBag] as on the shelf. */
    @Composable
    fun Archive(onBack: () -> Unit, onOpenBag: (bagId: String) -> Unit, modifier: Modifier = Modifier)

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
        modifier: Modifier = Modifier,
        brew: (@Composable (coffee: ShelfBrewCoffee, onClose: (saved: Boolean) -> Unit) -> Unit)? = null,
    )

    /** Scan a bag and put it on the shelf, as a full-screen destination; [onSaved] after the new bag is on the shelf. */
    @Composable
    fun AddBag(onSaved: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier)
}

/** The coffee a home check-in starts with: a catalogue [slug], or else the bag's own [name]; both null when the bag has neither. */
data class ShelfBrewCoffee(val slug: String?, val name: String?)
