package com.coffeepeek.feature.shop.api

import androidx.compose.runtime.Composable

/** A modal entry, not a root route. Composition owns account gating and destinations. */
interface ShopCheckInCreateEntry {
    @Composable
    fun Content(
        shopId: String,
        shopSlug: String,
        shopName: String,
        onDismiss: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
    )
}
