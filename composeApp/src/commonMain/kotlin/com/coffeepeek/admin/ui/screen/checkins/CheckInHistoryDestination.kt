package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

/** Both platforms select the migrated history entry; legacy remains a fallback. */
@Composable
internal fun CheckInHistoryDestination(initialHomeId: String? = null,
    renderer: CheckInHistoryScreenRenderer = koinInject()) {
    renderer.Content(initialHomeId)
}
