package com.coffeepeek.feature.shelf.api

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The shelf screens. The application owns the tab, the root back stack, sign-in and the catalog. */
interface ShelfEntry {
    /**
     * The shelf inside the Coffee tab. [header] is the app's content above the shelf (the "Моя полка / Каталог" switch);
     * a guest ([signedIn] false) sees empty shelves and an invitation to sign in. [onAddBag] opens the scan from the empty shelf.
     */
    @Composable
    fun Shelf(
        signedIn: Boolean,
        onOpenArchive: () -> Unit,
        onBrowseCatalog: () -> Unit,
        onSignIn: () -> Unit,
        onAddBag: () -> Unit,
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
        header: @Composable ColumnScope.() -> Unit = {},
    )

    /** Finished bags as a full-screen destination. */
    @Composable
    fun Archive(onBack: () -> Unit, modifier: Modifier = Modifier)

    /** Scan a bag and put it on the shelf, as a full-screen destination; [onSaved] after the new bag is on the shelf. */
    @Composable
    fun AddBag(onSaved: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier)
}
