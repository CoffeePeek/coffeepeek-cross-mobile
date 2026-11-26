package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.impl.*
import com.coffeepeek.feature.shop.impl.ui.compose.component.dayName
import com.coffeepeek.feature.shop.impl.ui.compose.model.*
import com.coffeepeek.feature.shop.impl.ui.data.*
import com.coffeepeek.feature.shop.impl.resources.*

internal suspend fun ShopDetailIosResources.shopDetailIosState(state: ShopDetailState, distance: String?, formatDate: (String) -> String,
    formatMenu: (String) -> String, message: String?, language: String): ShopDetailIosState {
    val snapshot = detailCheckInSnapshot(state, language)
    return ShopDetailIosState(state.sessionGeneration, state.isLoading, state.hasError, state.isLoggedIn,
        state.isFavorite, state.favoriteAvailable || state.selectCollectionsOnFavorite, state.isFavoriteLoading,
        state.selectCollectionsOnFavorite, text(when {
            state.isFavoriteLoading -> Res.string.shop_favorite_updating
            state.selectCollectionsOnFavorite -> Res.string.shop_favorite_choose_collections
            !state.favoriteAvailable -> Res.string.shop_favorite_unavailable
            state.isFavorite -> Res.string.shop_favorite_remove
            else -> Res.string.shop_favorite_add
        }), state.scheduleExpanded, state.featuresExpanded, state.priceInfoVisible, message,
        state.details?.let { iosShopDetails(it, state, distance, formatMenu) }, snapshot,
        shopDetailIosLabels() + ("checkins-empty" to text(Res.string.shop_checkins_empty, state.details?.overview?.title.orEmpty())),
        snapshot.rows.associate { it.id to formatDate(it.date) })
}

private suspend fun ShopDetailIosResources.iosShopDetails(details: ShopDetails, state: ShopDetailState, distance: String?,
    formatMenu: (String) -> String): ShopDetailIosDetails {
    val shop = details.overview
    val price = ShopPriceLevel.from(shop.priceRange)
    return ShopDetailIosDetails(shop.id, shop.canonicalPath?.substringAfterLast('/') ?: shop.id, shop.title,
        shop.description?.trim()?.takeIf(String::isNotEmpty), shop.address?.takeIf(String::isNotBlank), distance, text(kind(shop.type)),
        shop.rating?.let(::formatShopRating) ?: text(Res.string.shop_overview_no_rating),
        text(coffeePeekPluralForm(shop.reviewCount, Res.string.shop_overview_reviews_one,
            Res.string.shop_overview_reviews_few, Res.string.shop_overview_reviews), shop.reviewCount),
        shop.isOpen, shop.latitude != null && shop.longitude != null,
        shop.photos.map { ShopDetailIosPhoto(it.id, it.previewUrl, text(Res.string.shop_overview_photo_description, shop.title)) },
        details.coffee.roasters.map { ShopDetailIosRoaster(it.id, it.name, it.photoUrl,
            text(Res.string.shop_coffee_open_roaster, it.name)) }, details.coffee.beans, details.coffee.equipment,
        status(shopScheduleStatus(details.schedules, shop.isOpen, state.todayDayOfWeek, state.todayMinuteOfDay)),
        details.schedules.sortedBy { if (it.dayOfWeek == 0) 7 else it.dayOfWeek }.map {
            ShopDetailIosSchedule(text(dayName(it.dayOfWeek)), hours(it), it.dayOfWeek == state.todayDayOfWeek)
        }, details.menu?.takeIf { it.photos.isNotEmpty() || groupedPresentMenuItems(it.items).isNotEmpty() }?.let { iosMenu(it, formatMenu) },
        iosContacts(details.contact), details.features.map { feature(it) }, featureToggle(details.features.size, state.featuresExpanded),
        price?.let { text(priceValue(it)) }, priceTiers(price))
}

private fun kind(type: ShopType) = when (type) {
    ShopType.Specialty -> Res.string.shop_type_specialty
    ShopType.CoffeeBar -> Res.string.shop_type_coffee_bar
    ShopType.Cafe -> Res.string.shop_type_cafe
}

internal fun detailCheckInSnapshot(state: ShopDetailState, language: String): ShopCheckInListSnapshot {
    val details = state.details
    val all = details?.checkIns.orEmpty()
    val shown = all.take(if (state.isLoggedIn) 3 else 1)
    val list = ShopCheckInListState(state.sessionGeneration, state.isLoading, details != null,
        state.hasError, false, details?.overview?.title.orEmpty(), details?.overview?.rating,
        details?.overview?.reviewCount ?: 0, all.size, all.ratingSummary(), shown,
        (all.take(3).size - shown.size).coerceAtLeast(0), details?.userCheckIns.orEmpty().mapTo(mutableSetOf()) { it.id },
        state.isLoggedIn, state.currentUserId, state.pendingCheckInVoteId)
    return ShopCheckInListSnapshot(list, language)
}

private suspend fun ShopDetailIosResources.featureToggle(count: Int, expanded: Boolean): String = if (expanded) text(Res.string.shop_features_collapse)
    else text(coffeePeekPluralForm(count, Res.string.shop_features_show_all_count_one,
        Res.string.shop_features_show_all_count_few, Res.string.shop_features_show_all_count_many), count)

private fun priceValue(level: ShopPriceLevel) = when (level) {
    ShopPriceLevel.Low -> Res.string.shop_price_low_value
    ShopPriceLevel.Average -> Res.string.shop_price_average_value
    ShopPriceLevel.High -> Res.string.shop_price_high_value
}

private suspend fun ShopDetailIosResources.priceTiers(selected: ShopPriceLevel?): List<ShopDetailIosPriceTier> = ShopPriceLevel.entries.map {
    ShopDetailIosPriceTier(it.indicatorCount, text(when (it) {
        ShopPriceLevel.Low -> Res.string.shop_price_low_hint
        ShopPriceLevel.Average -> Res.string.shop_price_average_hint
        ShopPriceLevel.High -> Res.string.shop_price_high_hint
    }), it == selected)
}

/** Application disposal must clear every renderable protected row together with the detail snapshot. */
fun ShopDetailIosState.withoutContent(languageTag: String): ShopDetailIosState = copy(
    details = null, message = null, priceInfoVisible = false, checkInDates = emptyMap(),
    checkIns = ShopCheckInListSnapshot(ShopCheckInListState(sessionGeneration = generation), languageTag),
)
