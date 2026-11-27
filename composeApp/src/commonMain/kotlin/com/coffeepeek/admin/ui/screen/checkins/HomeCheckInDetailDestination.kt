package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

/** Home detail/edit owns a route; the unified history remains in Profile. */
@Composable
internal fun HomeCheckInDetailDestination(id: String, editOnLoad: Boolean = false,
    renderer: HomeCheckInDetailRenderer = koinInject()) {
    renderer.Content(id, editOnLoad)
}
