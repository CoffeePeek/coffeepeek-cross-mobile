package com.coffeepeek.feature.checkinhistory.api

import androidx.compose.runtime.Composable

/** Signed-in profile scope: one calendar/filter/paged history survives profile tab changes.
 * The caller supplies an account-bound lifetime and owns navigation. The list owns scrolling;
 * its header slot contains the profile header, calendar and tabs. Selection revision advances
 * on calendar date selection so the caller can reveal its check-ins tab. */
interface ProfileHistoryEntry {
    @Composable
    fun Content(
        sessionGeneration: Int,
        contentGeneration: Int,
        showAllRequested: Boolean,
        onShowAllConsumed: () -> Unit,
        onOpenHome: (id: String) -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onOpenCheckIn: (id: String) -> Unit,
        onOpenPhoto: (urls: List<String>, originals: List<String>, index: Int) -> Unit,
        content: @Composable (
            calendar: @Composable () -> Unit,
            list: @Composable (header: @Composable () -> Unit) -> Unit,
            selectionRevision: Int,
        ) -> Unit,
    )
}
