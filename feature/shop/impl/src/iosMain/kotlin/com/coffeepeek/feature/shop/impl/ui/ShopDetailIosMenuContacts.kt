package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.impl.ui.data.*
import com.coffeepeek.feature.shop.impl.resources.*

internal suspend fun ShopDetailIosResources.iosMenu(menu: ShopMenu, formatDate: (String) -> String): ShopDetailIosMenu {
    val captured = menu.capturedAtUtc?.takeIf(String::isNotBlank)?.let(formatDate)
    val updated = menu.updatedAtUtc?.takeIf(String::isNotBlank)?.let(formatDate)
    val freshness = listOfNotNull(
        captured?.let { text(Res.string.shop_menu_captured, it) },
        updated?.takeUnless { it == captured }?.let { text(Res.string.shop_menu_updated, it) })
    return ShopDetailIosMenu(groupedPresentMenuItems(menu.items).map { items -> items.map { iosMenuItem(it) } },
        menu.photos.mapIndexed { i, photo -> ShopDetailIosPhoto(photo.id, photo.previewUrl,
            text(Res.string.shop_menu_gallery_photo_description, i + 1, menu.photos.size)) }, freshness)
}

private suspend fun ShopDetailIosResources.iosMenuItem(item: ShopMenuItem): ShopDetailIosMenuItem {
    val primary = if (language.substringBefore('-') == "en") item.nameEn else item.nameRu
    val fallback = if (language.substringBefore('-') == "en") item.nameRu else item.nameEn
    val amount = item.price?.takeIf(Double::isFinite)?.let(::formatShopMenuAmount)
    val byn = item.currency.isBlank() || item.currency.equals("BYN", ignoreCase = true)
    val priceLabel = amount?.let {
        if (byn) text(Res.string.shop_menu_byn_price_description, it) else "$it ${item.currency}"
    }
    return ShopDetailIosMenuItem(item.slug, primary.ifBlank { fallback.ifBlank { item.slug } }, amount,
        if (byn) "BYN" else item.currency, priceLabel)
}

internal suspend fun ShopDetailIosResources.iosContacts(contact: ShopContact?): List<ShopDetailIosContact> {
    if (contact == null) return emptyList()
    return listOfNotNull(
        phoneLink(contact.phone)?.let { iosContact("phone", it, true) },
        instagramLink(contact.instagram)?.let { iosContact("instagram", it) },
        websiteLink(contact.website)?.let { iosContact("website", it) },
        emailLink(contact.email)?.let { iosContact("email", it) },
    )
}

private suspend fun ShopDetailIosResources.iosContact(kind: String, link: ContactLink, copy: Boolean = false) =
    ShopDetailIosContact(kind, link.label, link.target, copy, text(Res.string.shop_contact_open, link.label))
