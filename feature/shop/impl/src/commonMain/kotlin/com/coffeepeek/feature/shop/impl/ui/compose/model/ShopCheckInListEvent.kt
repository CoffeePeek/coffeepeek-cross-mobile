package com.coffeepeek.feature.shop.impl.ui.compose.model

internal data class ShopCheckInListEventEnvelope(val event: ShopCheckInListEvent, val sessionGeneration: Int)

internal sealed interface ShopCheckInListEvent {
    data object Back : ShopCheckInListEvent
    data object SignIn : ShopCheckInListEvent
    data object Register : ShopCheckInListEvent
    data class Report(val checkInId: String) : ShopCheckInListEvent
    data class OpenAuthor(val slug: String, val nickname: String, val avatarUrl: String?) : ShopCheckInListEvent
    data class OpenPhoto(val urls: List<String>, val index: Int, val originals: List<String> = urls) : ShopCheckInListEvent
}
