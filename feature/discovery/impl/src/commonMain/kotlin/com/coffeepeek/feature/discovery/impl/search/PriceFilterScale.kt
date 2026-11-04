package com.coffeepeek.feature.discovery.impl.search

import com.coffeepeek.feature.discovery.domain.model.PriceLevel

/** Current search API has three bands. Legacy Luxury belongs to Expensive, never a separate search option. */
internal val priceFilterStops: List<PriceLevel?> = listOf(null, PriceLevel.Cheap, PriceLevel.Moderate, PriceLevel.Expensive)

internal fun priceFilterStop(level: PriceLevel?): Int = when (level) {
    null -> 0
    PriceLevel.Cheap -> 1
    PriceLevel.Moderate -> 2
    PriceLevel.Expensive, PriceLevel.Luxury -> 3
}
