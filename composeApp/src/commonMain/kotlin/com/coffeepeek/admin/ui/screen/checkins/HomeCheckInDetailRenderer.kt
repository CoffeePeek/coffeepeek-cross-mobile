package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable

/** App-owned detail/edit assembly, without a separate history page. */
interface HomeCheckInDetailRenderer {
    @Composable
    fun Content(id: String, editOnLoad: Boolean)
}
