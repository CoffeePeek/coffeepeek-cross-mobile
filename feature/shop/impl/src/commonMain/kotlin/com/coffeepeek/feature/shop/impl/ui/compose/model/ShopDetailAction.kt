package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopDetailAction {
    data object Back : ShopDetailAction
    data object Retry : ShopDetailAction
    data object OpenMap : ShopDetailAction
    data object OpenMenuGallery : ShopDetailAction
    data object ToggleSchedule : ShopDetailAction
    data object ToggleFeatures : ShopDetailAction
    data object SignIn : ShopDetailAction
    data object Register : ShopDetailAction
    data object ToggleFavorite : ShopDetailAction
    data class OpenPhoto(val urls: List<String>, val index: Int) : ShopDetailAction
    data class OpenRoaster(val id: String) : ShopDetailAction
    data class OpenLink(val target: String) : ShopDetailAction
    data class CopyPhone(val number: String) : ShopDetailAction
    data class VoteHelpful(val reviewId: String) : ShopDetailAction
}
