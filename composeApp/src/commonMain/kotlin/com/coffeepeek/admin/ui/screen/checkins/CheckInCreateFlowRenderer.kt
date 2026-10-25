package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable

/** Platform composition selects Home without eagerly constructing the shared legacy ViewModel. */
interface CheckInCreateFlowRenderer {
    @Composable
    fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit,
        legacyContent: @Composable () -> Unit)
}

/** Legacy fallback retained until both platform migrations are accepted. */
internal object LegacyCheckInCreateFlowRenderer : CheckInCreateFlowRenderer {
    @Composable
    override fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit,
        legacyContent: @Composable () -> Unit) = legacyContent()
}
