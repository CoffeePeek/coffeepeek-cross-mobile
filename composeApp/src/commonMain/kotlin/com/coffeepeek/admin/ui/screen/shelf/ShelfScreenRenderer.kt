package com.coffeepeek.admin.ui.screen.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.Navigator

/** The shelf's full-screen destinations. Android composes the shelf feature; iOS gets the shelf in a later stage. */
interface ShelfScreenRenderer {
    @Composable
    fun Archive()
}

/** Nothing on iOS opens the archive yet; if the route is reached anyway, it leaves at once. */
internal object UnsupportedShelfScreenRenderer : ShelfScreenRenderer {
    @Composable
    override fun Archive() = LaunchedEffect(Unit) { Navigator.popBack() }
}
