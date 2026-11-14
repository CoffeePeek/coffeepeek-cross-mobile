package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Platform composition selects the editor; the app keeps auth and destinations. */
interface ShopCheckInFormRenderer {
    @Composable
    fun Content(
        shopId: String,
        shopSlug: String?,
        shopName: String,
        shopAddress: String?,
        onDismiss: () -> Unit,
        onChangeShop: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
    )
}
