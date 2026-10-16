package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopReviewFormEvent {
    data object Submitted : ShopReviewFormEvent
    data object DraftClearFailed : ShopReviewFormEvent
}
