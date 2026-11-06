package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

/** Platform-selected global flow. */
@Composable
internal fun CheckInCreateFlow(
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit = {},
    renderer: CheckInCreateFlowRenderer = koinInject(),
) {
    renderer.Content(onDismiss, onShop, onHomeSaved)
}
