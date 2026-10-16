package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.api.ShopDetailContext

internal data class ShopDetailEventEnvelope(val event: ShopDetailEvent, val sessionGeneration: Int)

internal sealed interface ShopDetailEvent {
    data object Back : ShopDetailEvent
    data class OpenMenuGallery(val shopId: String) : ShopDetailEvent
    data object SignIn : ShopDetailEvent
    data object Register : ShopDetailEvent
    data object VoteFailed : ShopDetailEvent
    data object FavoriteFailed : ShopDetailEvent
    data class CreateCheckIn(val shop: ShopDetailContext) : ShopDetailEvent
    data object ChooseAnotherShop : ShopDetailEvent
    data class ReportShop(val shop: ShopDetailContext) : ShopDetailEvent
    data class OpenCheckIns(val shopId: String) : ShopDetailEvent
    data class ReportCheckIn(val checkInId: String) : ShopDetailEvent
    data class OpenAuthor(val slug: String, val nickname: String, val avatarUrl: String?) : ShopDetailEvent
    data class FavoriteChanged(val shopId: String, val isFavorite: Boolean) : ShopDetailEvent
    data class OpenMap(val latitude: Double, val longitude: Double, val title: String) : ShopDetailEvent
    data class OpenRoute(val latitude: Double, val longitude: Double) : ShopDetailEvent
    data class ShareShop(val canonicalPath: String, val title: String) : ShopDetailEvent
    data class SuggestChange(val shop: ShopDetailContext) : ShopDetailEvent
    data class OpenPhoto(val urls: List<String>, val index: Int) : ShopDetailEvent
    data class OpenRoaster(val id: String) : ShopDetailEvent
    data class OpenLink(val target: String) : ShopDetailEvent
    data class CopyPhone(val number: String) : ShopDetailEvent
}
