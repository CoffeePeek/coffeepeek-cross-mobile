package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopCheckInListAction {
    data object Retry : ShopCheckInListAction
    data object Resume : ShopCheckInListAction
    data object Back : ShopCheckInListAction
    data object SignIn : ShopCheckInListAction
    data object Register : ShopCheckInListAction
    data class SessionChanged(val generation: Int) : ShopCheckInListAction
    data class Refresh(val generation: Long) : ShopCheckInListAction
    data class ToggleHelpful(val checkInId: String) : ShopCheckInListAction
    data class Report(val checkInId: String) : ShopCheckInListAction
    data class OpenAuthor(val checkInId: String) : ShopCheckInListAction
    data class BlockAuthor(val checkInId: String) : ShopCheckInListAction
    data class OpenPhoto(val checkInId: String, val index: Int) : ShopCheckInListAction
}
