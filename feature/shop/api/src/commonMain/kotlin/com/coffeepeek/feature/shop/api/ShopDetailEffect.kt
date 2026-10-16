package com.coffeepeek.feature.shop.api

/** Application-owned navigation/platform actions. No feature ViewModels or backend models escape. */
sealed interface ShopDetailEffect {
    data object Back : ShopDetailEffect
    data object ChooseAnotherShop : ShopDetailEffect
    data object SignIn : ShopDetailEffect
    data object Register : ShopDetailEffect
    data class OpenMenuGallery(val shopId: String) : ShopDetailEffect
    data class CreateCheckIn(val shop: ShopDetailContext) : ShopDetailEffect
    data class ReportShop(val shop: ShopDetailContext) : ShopDetailEffect
    data class OpenCheckIns(val shopId: String) : ShopDetailEffect
    data class ReportCheckIn(val checkInId: String) : ShopDetailEffect
    data class OpenAuthor(val slug: String, val nickname: String, val avatarUrl: String?) : ShopDetailEffect
    data class FavoriteChanged(val shopId: String, val isFavorite: Boolean) : ShopDetailEffect
    data class OpenMap(val latitude: Double, val longitude: Double, val title: String) : ShopDetailEffect
    data class OpenRoute(val latitude: Double, val longitude: Double) : ShopDetailEffect
    data class ShareShop(val canonicalPath: String, val title: String) : ShopDetailEffect
    data class SuggestChange(val shop: ShopDetailContext) : ShopDetailEffect
    data class OpenPhoto(val imageUrls: List<String>, val initialIndex: Int) : ShopDetailEffect
    data class OpenRoaster(val slug: String) : ShopDetailEffect
    data class OpenLink(val target: String) : ShopDetailEffect
    data class CopyPhone(val number: String) : ShopDetailEffect
}
