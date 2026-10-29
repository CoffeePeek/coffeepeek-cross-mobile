package com.coffeepeek.feature.coffee.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * App-owned tab/navigation, floating-navigation clearance and [header] content above the search; no root router in the feature.
 *
 * No default arguments: Kotlin/Native links an implementation in another module against the abstract Composable's exact
 * signature, and Compose makes defaulted parameters of the override nullable, so defaults would break iOS.
 */
interface CoffeeListEntry {
    @Composable
    fun Content(
        onOpenCoffee: (slug: String) -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
        header: @Composable () -> Unit,
    )
}
