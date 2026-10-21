package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

/** Platform selection keeps the current iOS screen while Android uses the migrated entry. */
@Composable
internal fun CheckInHistoryDestination(initialHomeId: String? = null,
    renderer: CheckInHistoryScreenRenderer = koinInject()) {
    renderer.Content(initialHomeId)
}
