package com.coffeepeek.feature.shop.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.impl.*
import com.coffeepeek.feature.shop.impl.ui.compose.component.dayName
import com.coffeepeek.feature.shop.impl.ui.compose.component.scheduleHours
import com.coffeepeek.feature.shop.impl.ui.compose.component.scheduleStatusText
import com.coffeepeek.feature.shop.impl.ui.compose.model.*
import com.coffeepeek.feature.shop.impl.ui.data.*
import com.coffeepeek.feature.shop.impl.resources.*

@Composable
internal fun shopDetailIosState(state: ShopDetailState, distance: String?, formatDate: (String) -> String,
    formatMenu: (String) -> String, message: String?, language: String): ShopDetailIosState {
    val snapshot = detailCheckInSnapshot(state, language)
    return ShopDetailIosState(state.sessionGeneration, state.isLoading, state.hasError, state.isLoggedIn,
        state.isFavorite, state.favoriteAvailable || state.selectCollectionsOnFavorite, state.isFavoriteLoading,
        state.selectCollectionsOnFavorite, stringResource(when {
            state.isFavoriteLoading -> Res.string.shop_favorite_updating
            state.selectCollectionsOnFavorite -> Res.string.shop_favorite_choose_collections
            !state.favoriteAvailable -> Res.string.shop_favorite_unavailable
            state.isFavorite -> Res.string.shop_favorite_remove
            else -> Res.string.shop_favorite_add
        }), state.scheduleExpanded, state.featuresExpanded, state.priceInfoVisible, message,
        state.details?.let { iosShopDetails(it, state, distance, formatMenu) }, snapshot,
        shopDetailIosLabels() + ("checkins-empty" to stringResource(Res.string.shop_checkins_empty, state.details?.overview?.title.orEmpty())),
        snapshot.rows.associate { it.id to formatDate(it.date) })
}

@Composable
private fun iosShopDetails(details: ShopDetails, state: ShopDetailState, distance: String?,
    formatMenu: (String) -> String): ShopDetailIosDetails {
    val shop = details.overview
    val price = ShopPriceLevel.from(shop.priceRange)
    return ShopDetailIosDetails(shop.id, shop.canonicalPath?.substringAfterLast('/') ?: shop.id, shop.title,
        shop.description?.trim()?.takeIf(String::isNotEmpty), shop.address?.takeIf(String::isNotBlank), distance, stringResource(when (shop.type) {
            ShopType.Specialty -> Res.string.shop_type_specialty
            ShopType.CoffeeBar -> Res.string.shop_type_coffee_bar
            ShopType.Cafe -> Res.string.shop_type_cafe
        }), shop.rating?.let(::formatShopRating) ?: stringResource(Res.string.shop_overview_no_rating),
        stringResource(coffeePeekPluralForm(shop.reviewCount, Res.string.shop_overview_reviews_one,
            Res.string.shop_overview_reviews_few, Res.string.shop_overview_reviews), shop.reviewCount),
        shop.isOpen, shop.latitude != null && shop.longitude != null,
        shop.photos.map { ShopDetailIosPhoto(it.id, it.previewUrl, stringResource(Res.string.shop_overview_photo_description, shop.title)) },
        details.coffee.roasters.map { ShopDetailIosRoaster(it.id, it.name, it.photoUrl,
            stringResource(Res.string.shop_coffee_open_roaster, it.name)) }, details.coffee.beans, details.coffee.equipment,
        scheduleStatusText(shopScheduleStatus(details.schedules, shop.isOpen, state.todayDayOfWeek, state.todayMinuteOfDay)),
        details.schedules.sortedBy { if (it.dayOfWeek == 0) 7 else it.dayOfWeek }.map {
            ShopDetailIosSchedule(stringResource(dayName(it.dayOfWeek)), scheduleHours(it), it.dayOfWeek == state.todayDayOfWeek)
        }, details.menu?.takeIf { it.photos.isNotEmpty() || groupedPresentMenuItems(it.items).isNotEmpty() }?.let { iosMenu(it, formatMenu) },
        iosContacts(details.contact), details.features.map { iosFeature(it) }, featureToggle(details.features.size, state.featuresExpanded),
        price?.let { stringResource(priceValue(it)) }, priceTiers(price))
}

@Composable
private fun iosFeature(feature: com.coffeepeek.feature.shop.domain.model.ShopFeature) = ShopDetailIosFeature(
    feature.name,
    com.coffeepeek.core.designsystem.component.coffeePeekNativeImage(
        com.coffeepeek.feature.shop.impl.ui.compose.component.shopFeaturePainter(feature), size = 28f),
)

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

@Composable
private fun featureToggle(count: Int, expanded: Boolean): String = if (expanded) stringResource(Res.string.shop_features_collapse)
    else stringResource(coffeePeekPluralForm(count, Res.string.shop_features_show_all_count_one,
        Res.string.shop_features_show_all_count_few, Res.string.shop_features_show_all_count_many), count)

private fun priceValue(level: ShopPriceLevel) = when (level) {
    ShopPriceLevel.Low -> Res.string.shop_price_low_value
    ShopPriceLevel.Average -> Res.string.shop_price_average_value
    ShopPriceLevel.High -> Res.string.shop_price_high_value
}

@Composable
private fun priceTiers(selected: ShopPriceLevel?): List<ShopDetailIosPriceTier> = ShopPriceLevel.entries.map {
    ShopDetailIosPriceTier(it.indicatorCount, stringResource(when (it) {
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
