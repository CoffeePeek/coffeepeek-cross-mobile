package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.shop.api.*

/** Keeps the existing consent-aware wire contract at the application boundary. */
internal object AndroidShopAnalytics : ShopAnalytics {
    override fun action(action: ShopActionMetric) {
        Analytics.shopAction(action.wire(), action.placement())
    }

    override fun gate(action: ShopActionMetric) {
        Analytics.gate(if (action == ShopActionMetric.Edit) "edit_shop" else "report_issue")
    }

    override fun checkInStarted(authenticated: Boolean) {
        Analytics.shopAction("check_in")
        Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "shop_detail")
        if (!authenticated) Analytics.gate("check_in")
    }

    override fun favoriteRequiresSignIn(desired: Boolean) {
        Analytics.track(AnalyticsEvent.FAVORITE_ATTEMPTED, "entity_type" to "coffee_shop", "is_favorite" to desired)
        Analytics.gate("favorites")
    }

    override fun content(section: ShopContentMetric, action: ShopContentAction) {
        Analytics.track(AnalyticsEvent.SHOP_CONTENT_INTERACTED,
            "section" to section.name.lowercase(), "action" to action.name.lowercase())
        if (section == ShopContentMetric.Roasters && action == ShopContentAction.Select) {
            DetailEntryMemory.record(Entity.Roaster, ResultSource.Related, null)
        }
    }

    @Composable override fun TrackDetail(loaded: Boolean) = TrackEntityDetail(Entity.Shop, loaded)

    @Composable override fun exposure(modifier: Modifier, feature: ShopExposure): Modifier =
        modifier.analyticsExposure(feature.wire(), "shop_detail", "shop_detail")
}

private fun ShopActionMetric.wire(): String = when (this) {
    ShopActionMetric.Edit -> "edit"
    ShopActionMetric.Report -> "report_issue"
    ShopActionMetric.Directions -> "directions"
    ShopActionMetric.Share -> "share"
    ShopActionMetric.PhoneCopy -> "phone_copy"
    ShopActionMetric.PhoneCall -> "phone_call"
    ShopActionMetric.Instagram -> "instagram"
    ShopActionMetric.Website -> "website"
    ShopActionMetric.Email -> "email"
}

private fun ShopActionMetric.placement(): String = when (this) {
    ShopActionMetric.Share -> "shop_header"
    ShopActionMetric.Directions -> "mobile_actions"
    ShopActionMetric.PhoneCopy, ShopActionMetric.PhoneCall, ShopActionMetric.Instagram,
    ShopActionMetric.Website, ShopActionMetric.Email -> "shop_contacts"
    else -> "shop_detail"
}

private fun ShopExposure.wire(): String = when (this) {
    ShopExposure.Favorite -> "favorites"
    ShopExposure.Share -> "share"
    ShopExposure.CheckIn -> "check_in"
    ShopExposure.Directions -> "directions"
    else -> "shop_${name.lowercase()}"
}
