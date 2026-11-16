package com.coffeepeek.feature.checkinhistory.api

import androidx.compose.runtime.Composable

/** Owner-only read surfaces. Composition supplies a revocable account and a destination-owned store. */
interface ProfileActivityEntry {
    @Composable
    fun Calendar(
        isLoggedIn: Boolean, selectedDate: String?, onSelectDate: (String?) -> Unit,
        expanded: Boolean, title: @Composable (String) -> Unit,
    )
}
