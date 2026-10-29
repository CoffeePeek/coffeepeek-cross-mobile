package com.coffeepeek.admin.ui.screen.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.Navigator

/** The shelf's full-screen destinations; builds with the shelf compose the shelf feature. */
interface ShelfScreenRenderer {
    /** Whether this build has the shelf, so other screens may offer a way into it. */
    val available: Boolean get() = true

    @Composable
    fun Archive()

    @Composable
    fun AddBag()

    @Composable
    fun Bag(bagId: String)
}

/** Builds without the shelf open none of these routes; if one is reached anyway, it leaves at once. */
internal object UnsupportedShelfScreenRenderer : ShelfScreenRenderer {
    override val available: Boolean get() = false

    @Composable
    override fun Archive() = LaunchedEffect(Unit) { Navigator.popBack() }

    @Composable
    override fun AddBag() = LaunchedEffect(Unit) { Navigator.popBack() }

    @Composable
    override fun Bag(bagId: String) = LaunchedEffect(Unit) { Navigator.popBack() }
}
