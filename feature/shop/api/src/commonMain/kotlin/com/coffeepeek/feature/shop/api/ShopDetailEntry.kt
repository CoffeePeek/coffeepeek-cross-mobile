package com.coffeepeek.feature.shop.api

import androidx.compose.runtime.Composable

/** Navigation lifecycle belongs to the caller. Generations must increase within an entry lifetime. */
interface ShopDetailEntry {
    @Composable
    fun Content(
        shopId: String,
        sessionGeneration: Int,
        forCheckIn: Boolean,
        formatCheckInDate: (String) -> String,
        formatMenuDate: (String) -> String,
        formatDistance: (latitude: Double, longitude: Double) -> String?,
        onEffect: (ShopDetailEffect) -> Unit,
        refreshGeneration: Long,
    )
}
