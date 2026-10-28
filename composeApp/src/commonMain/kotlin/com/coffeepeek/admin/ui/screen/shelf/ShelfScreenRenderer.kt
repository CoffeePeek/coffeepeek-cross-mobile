package com.coffeepeek.admin.ui.screen.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.Navigator

/** The shelf's full-screen destinations. Android composes the shelf feature; iOS gets the shelf in a later stage. */
interface ShelfScreenRenderer {
    @Composable
    fun Archive()

    @Composable
    fun AddBag()

    @Composable
    fun Bag(bagId: String)
}

/** Nothing on iOS opens these routes yet; if one is reached anyway, it leaves at once. */
internal object UnsupportedShelfScreenRenderer : ShelfScreenRenderer {
    @Composable
    override fun Archive() = LaunchedEffect(Unit) { Navigator.popBack() }

    @Composable
    override fun AddBag() = LaunchedEffect(Unit) { Navigator.popBack() }

    @Composable
    override fun Bag(bagId: String) = LaunchedEffect(Unit) { Navigator.popBack() }
}
