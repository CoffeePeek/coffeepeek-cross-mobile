package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import kotlin.math.round

/** Presentation averages for the loaded sample, never claimed as server-wide aggregates. */
internal data class ShopCheckInRatingSummary(val coffee: Double, val service: Double, val place: Double) {
    val overall: Double get() = (coffee + service + place) / 3.0
}

internal fun List<ShopCheckIn>.ratingSummary(): ShopCheckInRatingSummary? {
    val ratings = mapNotNull { it.rating }
    if (ratings.isEmpty()) return null
    return ShopCheckInRatingSummary(ratings.map { it.coffee }.average(),
        ratings.map { it.service }.average(), ratings.map { it.place }.average())
}

internal fun formatShopRating(value: Double): String {
    val tenths = round(value * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}
