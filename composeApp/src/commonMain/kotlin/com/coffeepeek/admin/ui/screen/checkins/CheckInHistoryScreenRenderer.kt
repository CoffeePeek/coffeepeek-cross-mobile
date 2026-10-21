package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable

/** Application platform selection; this adapter does not change the root route or own business rules. */
interface CheckInHistoryScreenRenderer {
    @Composable
    fun Content(initialHomeId: String? = null)
}

internal object LegacyCheckInHistoryScreenRenderer : CheckInHistoryScreenRenderer {
    @Composable
    override fun Content(initialHomeId: String?) = ProfileCheckInsScreen(initialHomeId = initialHomeId)
}
