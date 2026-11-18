package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer

/** Nested/root composition paths host the same native controllers as the UIKit navigation stack. */
internal fun iosShelfScreenRenderer(): ShelfScreenRenderer = object : ShelfScreenRenderer {
    @Composable override fun Archive() {
        val create = iosShelfArchiveContent()
        val controller = remember { create() }
        NativeEditorControllerHost(controller, Modifier.fillMaxSize())
    }
    @Composable override fun Bag(bagId: String) = NativeShelfRoute(Navigator.Screen.ShelfBag(bagId))
    @Composable override fun AddBag() = NativeShelfRoute(Navigator.Screen.ShelfAddBag)
}

@Composable
private fun NativeShelfRoute(route: Navigator.Screen) {
    val create = checkNotNull(iosShelfContent()(route))
    val controller = remember(route) { create() }
    NativeEditorControllerHost(controller, Modifier.fillMaxSize())
}
