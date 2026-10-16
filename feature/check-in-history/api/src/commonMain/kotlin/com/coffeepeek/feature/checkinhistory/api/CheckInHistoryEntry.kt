package com.coffeepeek.feature.checkinhistory.api

import androidx.compose.runtime.Composable

/**
 * The caller owns navigation, account-bound lifetime and date-label formatting.
 * Session generation advances on account/auth changes, not same-account token renewal.
 * Content generation is a monotonic return/write signal; its initial value is a baseline.
 * Time formatting uses the caller's local-zone policy. Home/detail/create callbacks
 * delegate to the existing editor and shop picker; this read entry owns no writers.
 * Picker callbacks are one-off lifecycle notifications for application-owned flow
 * coordination; no analytics SDK, record or account data crosses this contract.
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
        formatCheckInTime: (timestamp: String) -> String,
        onOpenHome: (id: String) -> Unit,
        onCreateShop: () -> Unit,
        onCreateHome: () -> Unit,
        contentGeneration: Int = 0,
        onCreatePickerOpened: () -> Unit = {},
        onCreatePickerCancelled: () -> Unit = {},
    )
}
