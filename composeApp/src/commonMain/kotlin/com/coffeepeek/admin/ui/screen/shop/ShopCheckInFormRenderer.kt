package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Platform composition selects the modal; the app keeps auth and destinations. */
interface ShopCheckInFormRenderer {
    val usesLegacyDraft: Boolean

    @Composable
    fun Content(
        shopId: String,
        shopSlug: String?,
        shopName: String,
        onDismiss: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
    )
}
