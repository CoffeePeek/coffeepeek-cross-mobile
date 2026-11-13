package com.coffeepeek.feature.shop.api

import androidx.compose.runtime.Composable

/** Editor entry; composition owns account gating and destinations. */
interface ShopCheckInCreateEntry {
    @Composable
    fun Content(
        shopId: String,
        shopSlug: String,
        shopName: String,
        shopAddress: String?,
        onDismiss: () -> Unit,
        onChangeShop: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
    )
}
