package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Close transient layers and scroll the current list; text and selected filters stay intact. */
@Composable
internal fun DiscoveryReselectionEffect(
    ui: DiscoveryTabState, showsDiscovery: Boolean, requests: Flow<Unit>, onAction: (SearchAction) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val discovery by rememberUpdatedState(showsDiscovery)
    val action by rememberUpdatedState(onAction)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(ui, requests, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            requests.collectLatest {
                focus.clearFocus(force = true)
                keyboard?.hide()
                ui.showRoasters = false
                ui.mapExpanded = false
                action(SearchAction.CloseFilters)
                action(SearchAction.DismissLocationFailure)
                (if (discovery) ui.list else ui.results).scrollToItem(0)
            }
        }
    }
}
