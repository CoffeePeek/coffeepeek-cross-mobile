package com.coffeepeek.feature.coffee.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Caller owns destination lifetime, root navigation and platform browser/share actions. */
interface CoffeeDetailEntry {
    @Composable
    fun Content(
        slug: String,
        onBack: () -> Unit,
        onOpenCoffee: (slug: String) -> Unit,
        onOpenRoaster: (slug: String) -> Unit,
        onOpenSeller: (url: String) -> Unit,
        onShare: (name: String, canonicalPath: String) -> Unit,
        modifier: Modifier = Modifier,
    )
}
