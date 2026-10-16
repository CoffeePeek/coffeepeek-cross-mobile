package com.coffeepeek.feature.shop.api

import androidx.compose.runtime.Composable

/** The caller owns the navigation lifecycle, session generation and localized date formatting. */
interface ShopCheckInListEntry {
    @Composable
    fun Content(
        shopId: String,
        sessionGeneration: Int,
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onRegister: () -> Unit,
        onReportCheckIn: (checkInId: String) -> Unit,
        onOpenPhoto: (imageUrls: List<String>, initialIndex: Int) -> Unit,
        onOpenAuthor: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
        refreshGeneration: Long = 0,
    )
}
