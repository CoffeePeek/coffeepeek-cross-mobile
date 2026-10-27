package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import com.coffeepeek.feature.shelf.api.ShelfEntry

internal class AndroidShelfScreenRenderer(private val entry: ShelfEntry) : ShelfScreenRenderer {
    @Composable
    override fun Archive() = entry.Archive(onBack = Navigator::popBack)
}
