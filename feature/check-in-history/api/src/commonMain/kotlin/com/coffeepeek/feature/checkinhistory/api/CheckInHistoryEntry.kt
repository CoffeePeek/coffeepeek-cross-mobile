package com.coffeepeek.feature.checkinhistory.api

import androidx.compose.runtime.Composable

/**
 * The caller owns navigation, account-bound lifetime and date-label formatting.
 * Session generation advances on account/auth changes, not same-account token renewal.
 * Content generation is a monotonic return/write signal; its initial value is a baseline.
 */
interface CheckInHistoryEntry {
    @Composable
    fun Content(
        sessionGeneration: Int,
        isLoggedIn: Boolean,
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (shopSlug: String) -> Unit,
        onOpenPhoto: (imageUrls: List<String>, initialIndex: Int) -> Unit,
        contentGeneration: Int = 0,
    )
}
