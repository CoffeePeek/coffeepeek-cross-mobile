package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** The native Search tab uses the same non-destructive retap policy as Android. */
@Composable
internal fun FeedReselectionEffect(
    layout: FeedLayoutState, showsDiscovery: Boolean, requests: Flow<Unit>,
    dismissInput: () -> Unit, closeFilters: () -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val discovery by rememberUpdatedState(showsDiscovery)
    val dismiss by rememberUpdatedState(dismissInput)
    val close by rememberUpdatedState(closeFilters)
    LaunchedEffect(layout, requests, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            requests.collectLatest {
                dismiss()
                close()
                layout.mapExpanded = false
                (if (discovery) layout.discoveryListState else layout.listState).scrollToItem(0)
            }
        }
    }
}
