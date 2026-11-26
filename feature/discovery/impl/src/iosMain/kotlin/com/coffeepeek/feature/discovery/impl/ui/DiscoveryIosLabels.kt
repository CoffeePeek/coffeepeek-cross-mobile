package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/** Copy needed by the native navigation owner before a picker session can be prepared. */
@OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
suspend fun nativeDiscoveryPickerTitle(): String =
    getString(getSystemResourceEnvironment(), Res.string.discovery_picker_title)

internal suspend fun DiscoveryIosResources.discoveryIosLabels(picker: Boolean): Map<String, String> =
    (mainLabels(picker) + mapLabels() + feedbackLabels()).mapValues { text(it.value) }

private fun mainLabels(picker: Boolean): Map<String, StringResource> = mapOf(
    "title" to Res.string.discovery_picker_title,
    "search" to if (picker) Res.string.discovery_picker_search else Res.string.discovery_search_placeholder,
    "clear" to Res.string.discovery_search_clear, "cancel" to Res.string.discovery_search_cancel,
    "close" to Res.string.discovery_picker_close_description, "filters" to Res.string.discovery_filters,
    "shops" to Res.string.discovery_section_shops, "roasters" to Res.string.discovery_section_roasters,
    "show-all" to Res.string.discovery_show_all, "recent" to Res.string.discovery_picker_recent,
    "nearby" to Res.string.discovery_picker_nearby, "map" to Res.string.discovery_picker_map,
    "list" to Res.string.discovery_picker_list, "continue" to Res.string.discovery_continue_checkin,
    "open" to Res.string.discovery_shop_open, "closed" to Res.string.discovery_shop_closed,
    "new" to Res.string.discovery_shop_new,
)

private fun mapLabels(): Map<String, StringResource> = mapOf(
    "collapse-map" to Res.string.discovery_map_collapse, "zoom-in" to Res.string.discovery_map_zoom_in,
    "zoom-out" to Res.string.discovery_map_zoom_out, "location" to Res.string.discovery_map_my_location,
    "zones-show" to Res.string.discovery_map_zones_show, "zones-hide" to Res.string.discovery_map_zones_hide,
    "heatmap" to Res.string.discovery_heatmap_show, "heatmap-hide" to Res.string.discovery_heatmap_hide, "zone-close" to Res.string.discovery_map_zone_close,
    "zone-shops" to Res.string.discovery_map_zone_show_shops, "truncated" to Res.string.discovery_map_truncated,
    "map-failed" to Res.string.discovery_map_load_failed, "map-search-empty" to Res.string.discovery_map_search_empty,
    "map-search-failed" to Res.string.discovery_map_search_failed,
)

private fun feedbackLabels(): Map<String, StringResource> = mapOf(
    "loading" to Res.string.discovery_loading, "retry" to Res.string.discovery_retry,
    "failed" to Res.string.discovery_picker_failed, "more-failed" to Res.string.discovery_pagination_error,
    "refresh-failed" to Res.string.discovery_refresh_error, "favorite-failed" to Res.string.discovery_favorite_failed,
    "dismiss" to Res.string.discovery_message_dismiss, "settings" to Res.string.discovery_location_open_settings,
    "empty" to Res.string.discovery_picker_empty, "empty-Catalog" to Res.string.discovery_empty_catalog_body,
    "empty-Query" to Res.string.discovery_empty_query_body, "empty-Filters" to Res.string.discovery_empty_filters_body,
    "reset" to Res.string.discovery_reset_filters, "add-title" to Res.string.discovery_suggestion_title,
    "add-body" to Res.string.discovery_suggestion_description, "add" to Res.string.discovery_suggestion_action,
    "add-roaster" to Res.string.discovery_add_roaster,
    "add-roaster-title" to Res.string.discovery_roaster_suggestion_title,
    "add-roaster-body" to Res.string.discovery_roaster_suggestion_body,
    "empty-title-Catalog" to Res.string.discovery_empty_catalog_title,
    "empty-title-Query" to Res.string.discovery_empty_shops_title,
    "empty-title-Filters" to Res.string.discovery_empty_filters_title,
    "edit-query" to Res.string.discovery_edit_query,
)
