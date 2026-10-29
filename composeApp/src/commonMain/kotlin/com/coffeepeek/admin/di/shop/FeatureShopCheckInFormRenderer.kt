package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInFormRenderer
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry

internal class FeatureShopCheckInFormRenderer(
    private val entry: ShopCheckInCreateEntry,
) : ShopCheckInFormRenderer {
    override val usesLegacyDraft = false

    @Composable
    override fun Content(
        shopId: String,
        shopSlug: String?,
        shopName: String,
        onDismiss: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
        legacyContent: @Composable () -> Unit,
    ) {
        if (shopSlug.isNullOrBlank()) {
            LaunchedEffect(Unit) { onDismiss() }
            return
        }
        entry.Content(shopId, shopSlug, shopName, onDismiss, onSubmitted, onGoToFeed, onOpenHistory)
    }
}
