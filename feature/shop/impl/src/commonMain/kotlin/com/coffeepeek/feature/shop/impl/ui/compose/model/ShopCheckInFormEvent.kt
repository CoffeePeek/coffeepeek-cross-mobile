package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility

internal sealed interface ShopCheckInFormEvent {
    data object Dismiss : ShopCheckInFormEvent
    data object OpenHistory : ShopCheckInFormEvent
    data object GoToFeed : ShopCheckInFormEvent
    data class Submitted(val visibility: ShopCheckInVisibility) : ShopCheckInFormEvent
}
