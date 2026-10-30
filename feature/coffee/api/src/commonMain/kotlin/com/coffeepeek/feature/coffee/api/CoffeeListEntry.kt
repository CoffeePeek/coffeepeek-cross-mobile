package com.coffeepeek.feature.coffee.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.Flow

/**
 * App-owned tab/navigation, floating-navigation clearance and [header] content above the search; no root router in the feature.
 * [scrollToTopRequests] are non-replaying active-tab gestures; scrolling never changes the catalogue criteria.
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
        scrollToTopRequests: Flow<Unit>,
        animateScrollToTop: Boolean,
        header: @Composable () -> Unit,
    )
}
