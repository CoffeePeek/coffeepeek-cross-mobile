package com.coffeepeek.admin.ui.screen.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.Navigator

/** The shelf's full-screen destinations, supplied by each platform's application composition. */
interface ShelfScreenRenderer {
    @Composable
    fun Archive()

    @Composable
    fun AddBag()

    @Composable
    fun Bag(bagId: String)
}

/** Fallback for isolated composition graphs; production Android and iOS both supply the shelf feature. */
internal object UnsupportedShelfScreenRenderer : ShelfScreenRenderer {
    @Composable
    override fun Archive() = LaunchedEffect(Unit) { Navigator.popBack() }

    @Composable
    override fun AddBag() = LaunchedEffect(Unit) { Navigator.popBack() }

    @Composable
    override fun Bag(bagId: String) = LaunchedEffect(Unit) { Navigator.popBack() }
}
