package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.data.*

internal fun shopDetailIosIntent(name: String, id: String, index: Int, state: ShopDetailState): ShopDetailAction? =
    shopDetailSimpleIntent(name) ?: when (name) {
        "hero-photo" -> state.details?.overview?.photos?.takeIf { index in it.indices }?.let {
            ShopDetailAction.OpenPhoto(it.map { photo -> photo.previewUrl }, index, it.map { photo -> photo.fullUrl }) }
        "menu-photo" -> state.details?.menu?.photos?.takeIf { index in it.indices }?.let {
            ShopDetailAction.OpenPhoto(it.map { photo -> photo.previewUrl }, index, it.map { photo -> photo.fullUrl }) }
        "roaster" -> ShopDetailAction.OpenRoaster(id)
        "link" -> ShopDetailAction.OpenLink(id).takeIf { id in shopDetailContactTargets(state) }
        "copy" -> state.details?.contact?.phone?.let(::phoneLink)?.takeIf { it.label == id }?.let { ShopDetailAction.CopyPhone(it.label) }
        else -> detailCheckInIntent(name, id, index, state)
    }

private fun shopDetailSimpleIntent(name: String): ShopDetailAction? = when (name) {
    "back" -> ShopDetailAction.Back
    "retry" -> ShopDetailAction.Retry
    "map" -> ShopDetailAction.OpenMap
    "gallery" -> ShopDetailAction.OpenMenuGallery
    "schedule" -> ShopDetailAction.ToggleSchedule
    "features" -> ShopDetailAction.ToggleFeatures
    "sign-in" -> ShopDetailAction.SignIn
    "register" -> ShopDetailAction.Register
    "favorite" -> ShopDetailAction.ToggleFavorite
    "share" -> ShopDetailAction.Share
    "suggest" -> ShopDetailAction.SuggestChange
    "route" -> ShopDetailAction.OpenRoute
    "check-in" -> ShopDetailAction.OpenCheckIn
    "check-ins" -> ShopDetailAction.OpenCheckIns
    "report-shop" -> ShopDetailAction.ReportShop
    "price" -> ShopDetailAction.OpenPriceInfo
    "dismiss-price" -> ShopDetailAction.DismissPriceInfo
    else -> null
}

private fun detailCheckInIntent(name: String, id: String, index: Int, state: ShopDetailState): ShopDetailAction? {
    val visible = state.details?.checkIns.orEmpty().take(if (state.isLoggedIn) 3 else 1)
    if (visible.none { it.id == id }) return null
    return when (name) {
        "vote" -> ShopDetailAction.VoteCheckInHelpful(id)
        "report" -> ShopDetailAction.ReportCheckIn(id)
        "author" -> ShopDetailAction.OpenAuthor(id)
        "block" -> ShopDetailAction.BlockAuthor(id)
        "checkin-photo" -> ShopDetailAction.OpenCheckInPhoto(id, index)
        else -> null
    }
}

private fun shopDetailContactTargets(state: ShopDetailState): Set<String> {
    val contact = state.details?.contact ?: return emptySet()
    return listOfNotNull(phoneLink(contact.phone), emailLink(contact.email), websiteLink(contact.website),
        instagramLink(contact.instagram)).mapTo(mutableSetOf()) { it.target }
}
