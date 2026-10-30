package com.coffeepeek.feature.coffee.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Caller owns destination lifetime, root navigation and platform browser/share actions.
 *
 * No default arguments: Kotlin/Native links an implementation in another module against the abstract Composable's exact
 * signature, and Compose makes defaulted parameters of the override nullable, so defaults would break iOS.
 */
interface CoffeeDetailEntry {
    @Composable
    fun Content(
        slug: String,
        onBack: () -> Unit,
        onOpenCoffee: (slug: String) -> Unit,
        onOpenRoaster: (slug: String) -> Unit,
        onOpenSeller: (url: String) -> Unit,
        onShare: (name: String, canonicalPath: String) -> Unit,
        modifier: Modifier,
    )
}
