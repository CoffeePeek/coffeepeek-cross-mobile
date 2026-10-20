package com.coffeepeek.feature.shop.impl.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.shop.api.*
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState

internal val LocalShopAnalytics = staticCompositionLocalOf { ShopAnalytics.None }

@Composable
internal fun Modifier.shopExposure(feature: ShopExposure): Modifier =
    LocalShopAnalytics.current.exposure(this, feature)

internal fun trackShopAction(action: ShopDetailAction, state: ShopDetailState, analytics: ShopAnalytics) {
    if (state.details == null) return
    when (action) {
        ShopDetailAction.OpenCheckIn -> analytics.checkInStarted(state.isLoggedIn)
        ShopDetailAction.ToggleFavorite -> if (!state.isLoggedIn) analytics.favoriteRequiresSignIn(!state.isFavorite)
        ShopDetailAction.SuggestChange -> trackShopProtectedAction(ShopActionMetric.Edit, state, analytics)
        ShopDetailAction.ReportShop -> trackShopProtectedAction(ShopActionMetric.Report, state, analytics)
        ShopDetailAction.OpenRoute -> analytics.action(ShopActionMetric.Directions)
        ShopDetailAction.Share -> analytics.action(ShopActionMetric.Share)
        ShopDetailAction.ToggleSchedule -> analytics.content(ShopContentMetric.Hours,
            if (state.scheduleExpanded) ShopContentAction.Close else ShopContentAction.Open)
        is ShopDetailAction.OpenRoaster -> analytics.content(ShopContentMetric.Roasters, ShopContentAction.Select)
        is ShopDetailAction.CopyPhone -> analytics.action(ShopActionMetric.PhoneCopy)
        is ShopDetailAction.OpenLink -> trackShopContact(action.target, state, analytics)
        else -> Unit
    }
}

private fun trackShopProtectedAction(metric: ShopActionMetric, state: ShopDetailState, analytics: ShopAnalytics) {
    analytics.action(metric)
    if (!state.isLoggedIn) analytics.gate(metric)
}

private fun trackShopContact(target: String, state: ShopDetailState, analytics: ShopAnalytics) {
    val contact = state.details?.contact ?: return
    val metric = when (target) {
        phoneLink(contact.phone)?.target -> ShopActionMetric.PhoneCall
        instagramLink(contact.instagram)?.target -> ShopActionMetric.Instagram
        emailLink(contact.email)?.target -> ShopActionMetric.Email
        websiteLink(contact.website)?.target -> ShopActionMetric.Website
        else -> return
    }
    analytics.action(metric)
}
