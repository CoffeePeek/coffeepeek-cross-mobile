package com.coffeepeek.admin.ui.screen.shopchange

import androidx.compose.runtime.Composable

import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import coffeepeek.composeapp.generated.resources.*

import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.ShopChangeSection

@Composable
fun ModerationStatus.title(): String = when (this) {
    ModerationStatus.Pending -> stringResource(Res.string.legacy_my_contributions_screen_contributions_tab_pending)
    ModerationStatus.Approved -> stringResource(Res.string.legacy_shop_change_labels_approved)
    ModerationStatus.Rejected -> stringResource(Res.string.legacy_my_contributions_screen_contributions_tab_rejected)
}

@Composable
fun ShopChangeSection.localizedTitle(): String = when (this) {
    ShopChangeSection.Photos -> stringResource(Res.string.legacy_photo_attachments_section_contributions_section_photos)
    ShopChangeSection.Contacts -> stringResource(Res.string.legacy_add_shop_screen_contributions_section_contacts)
    ShopChangeSection.Description -> stringResource(Res.string.checkin_description_label)
    ShopChangeSection.Tags -> stringResource(Res.string.shop_filters_amenities)
    ShopChangeSection.Roasters -> stringResource(Res.string.favorites_roasters)
    ShopChangeSection.Equipment -> stringResource(Res.string.shop_filters_equipment)
    ShopChangeSection.Menu -> stringResource(Res.string.legacy_shop_detail_screen_contributions_section_menu)
    ShopChangeSection.BrewMethods -> stringResource(Res.string.legacy_shop_change_editor_screen_contributions_section_brew_methods)
}

@Composable
fun ShopChangeSection.localizedHint(): String = when (this) {
    ShopChangeSection.Photos -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_photos)
    ShopChangeSection.Contacts -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_contacts)
    ShopChangeSection.Description -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_description)
    ShopChangeSection.Tags -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_tags)
    ShopChangeSection.Roasters -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_roasters)
    ShopChangeSection.Equipment -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_equipment)
    ShopChangeSection.Menu -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_menu)
    ShopChangeSection.BrewMethods -> stringResource(Res.string.legacy_shop_change_labels_contributions_hint_brew_methods)
}
