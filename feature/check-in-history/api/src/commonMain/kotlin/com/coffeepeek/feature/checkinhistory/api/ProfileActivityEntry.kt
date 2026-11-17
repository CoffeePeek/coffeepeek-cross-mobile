package com.coffeepeek.feature.checkinhistory.api

import androidx.compose.runtime.Composable

/** Carries the month and its available navigation actions to the profile presentation. */
typealias ProfileActivityHeader = @Composable (month: String, previous: (() -> Unit)?, next: (() -> Unit)?) -> Unit

/** Owner-only read surfaces. Composition supplies a revocable account and a destination-owned store. */
interface ProfileActivityEntry {
    @Composable
    fun Calendar(
        isLoggedIn: Boolean, selectedDate: String?, onSelectDate: (String?) -> Unit,
        expanded: Boolean, header: ProfileActivityHeader,
    )
}
