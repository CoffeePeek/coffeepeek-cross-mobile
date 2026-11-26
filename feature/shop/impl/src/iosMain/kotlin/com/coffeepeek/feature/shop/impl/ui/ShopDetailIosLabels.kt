package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.impl.resources.*
import org.jetbrains.compose.resources.StringResource

internal suspend fun ShopDetailIosResources.shopDetailIosLabels(): Map<String, String> = (detailLabels() + sectionLabels() + priceLabels()).mapValues { text(it.value) }

private fun detailLabels(): Map<String, StringResource> = mapOf(
    "back" to Res.string.shop_detail_back, "dismiss" to Res.string.shop_detail_dismiss, "retry" to Res.string.shop_detail_retry,
    "loading" to Res.string.shop_detail_loading, "refreshing" to Res.string.shop_detail_refreshing,
    "error" to Res.string.shop_detail_load_error, "error-body" to Res.string.shop_detail_load_error_body,
    "share" to Res.string.shop_detail_share, "suggest" to Res.string.shop_detail_suggest_change,
    "route" to Res.string.shop_detail_route, "check-in" to Res.string.shop_detail_checkin,
    "map" to Res.string.shop_overview_show_on_map, "photos" to Res.string.shop_overview_photo_missing,
    "open" to Res.string.shop_overview_open, "closed" to Res.string.shop_overview_closed,
    "sign-in" to Res.string.shop_reviews_sign_in, "register" to Res.string.shop_reviews_register,
    "guest" to Res.string.shop_checkins_join_prompt, "no-account" to Res.string.shop_checkins_no_account,
)

private fun sectionLabels(): Map<String, StringResource> = mapOf(
    "description" to Res.string.shop_description_title, "roasters" to Res.string.shop_coffee_roasters,
    "beans" to Res.string.shop_coffee_beans, "equipment" to Res.string.shop_coffee_equipment,
    "hours" to Res.string.shop_schedule_title, "hours-expand" to Res.string.shop_schedule_expand,
    "hours-collapse" to Res.string.shop_schedule_collapse, "menu" to Res.string.shop_menu_title,
    "gallery" to Res.string.shop_menu_gallery_title, "contacts" to Res.string.shop_contact_title,
    "copy" to Res.string.shop_contact_copy_phone, "features" to Res.string.shop_features_title,
    "check-ins" to Res.string.shop_checkins_public_title, "all-check-ins" to Res.string.shop_checkins_show_all,
    "report" to Res.string.shop_report_prompt, "report-body" to Res.string.shop_report_hint,
)

private fun priceLabels(): Map<String, StringResource> = mapOf(
    "price" to Res.string.shop_price_title, "price-dismiss" to Res.string.shop_price_dismiss,
    "price-selected" to Res.string.shop_price_selected, "distance" to Res.string.shop_overview_distance,
)
