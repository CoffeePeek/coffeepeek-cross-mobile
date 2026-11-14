package com.coffeepeek.feature.discovery.api

import com.coffeepeek.feature.discovery.domain.model.ShopSummary

/** Session-bound recent places offered by composition to the check-in picker. */
fun interface DiscoveryRecentShops {
    suspend fun load(): Result<List<ShopSummary>>
}
