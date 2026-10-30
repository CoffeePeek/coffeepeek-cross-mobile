package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable

/** The global check-in creation flow; the feature owns Home form state. */
interface CheckInCreateFlowRenderer {
    @Composable
    fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit)
}
