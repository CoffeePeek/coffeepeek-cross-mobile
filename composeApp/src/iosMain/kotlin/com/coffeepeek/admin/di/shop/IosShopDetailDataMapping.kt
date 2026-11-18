package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosState
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosDetails
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosPhoto
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosRoaster
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosSchedule
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosMenu
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosMenuItem
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosContact
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosPriceTier

internal fun ShopDetailIosState.toNative() = IosShopDetailData(
    generation, loading, hasError,
    loggedIn, favorite, favoriteEnabled,
    favoriteBusy, favoriteSelection, favoriteLabel,
    scheduleExpanded, featuresExpanded, priceInfoVisible,
    message, details?.toNative(), labels,
    checkInDates,
)

internal fun ShopDetailIosDetails.toNative() = IosShopDetails(
    id, slug, title,
    description, address, distance,
    kind, rating, reviews,
    isOpen, canRoute, photos.map { it.toNative() },
    roasters.map { it.toNative() }, beans, equipment,
    scheduleStatus, schedule.map { it.toNative() }, menu?.toNative(),
    contacts.map { it.toNative() }, features, featureToggle,
    priceValue, priceTiers.map { it.toNative() },
)

internal fun ShopDetailIosPhoto.toNative() = IosShopDetailPhoto(
    id, url, label,
)

internal fun ShopDetailIosRoaster.toNative() = IosShopDetailRoaster(
    id, name, photo,
    label,
)

internal fun ShopDetailIosSchedule.toNative() = IosShopDetailSchedule(
    day, hours, today,
)

internal fun ShopDetailIosMenu.toNative() = IosShopDetailMenu(
    groups.map { group -> group.map { it.toNative() } }, photos.map { it.toNative() }, freshness,
)

internal fun ShopDetailIosMenuItem.toNative() = IosShopDetailMenuItem(
    id, title, amount,
    currency, priceLabel,
)

internal fun ShopDetailIosContact.toNative() = IosShopDetailContact(
    kind, title, target,
    copyable, label,
)

internal fun ShopDetailIosPriceTier.toNative() = IosShopDetailPriceTier(
    count, label, selected,
)
