package com.coffeepeek.feature.coffee.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** App-owned tab/navigation and floating-navigation clearance; no root router in the feature. */
interface CoffeeListEntry {
    @Composable
    fun Content(
        onOpenCoffee: (slug: String) -> Unit,
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
        /** Non-replaying active-tab gestures; scrolling never changes the catalogue criteria. */
        scrollToTopRequests: Flow<Unit> = emptyFlow(),
        animateScrollToTop: Boolean = true,
    )
}
