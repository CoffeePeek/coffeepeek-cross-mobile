package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopDetailEvent {
    data object Back : ShopDetailEvent
    data object OpenMenuGallery : ShopDetailEvent
    data object SignIn : ShopDetailEvent
    data object Register : ShopDetailEvent
    data object VoteFailed : ShopDetailEvent
    data object FavoriteFailed : ShopDetailEvent
    data object ReviewAccessUnavailable : ShopDetailEvent
    data object ReviewUnavailable : ShopDetailEvent
    data object CreateReview : ShopDetailEvent
    data class EditReview(val reviewId: String) : ShopDetailEvent
    data class FavoriteChanged(val shopId: String, val isFavorite: Boolean) : ShopDetailEvent
    data class OpenMap(val latitude: Double, val longitude: Double) : ShopDetailEvent
    data class OpenRoute(val latitude: Double, val longitude: Double) : ShopDetailEvent
    data class ShareShop(val shopId: String, val title: String) : ShopDetailEvent
    data class SuggestChange(val shopId: String) : ShopDetailEvent
    data class OpenPhoto(val urls: List<String>, val index: Int) : ShopDetailEvent
    data class OpenRoaster(val id: String) : ShopDetailEvent
    data class OpenLink(val target: String) : ShopDetailEvent
    data class CopyPhone(val number: String) : ShopDetailEvent
}
