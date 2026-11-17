package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

private val LocalIosFilterPresenter =
    staticCompositionLocalOf<(@Composable (DiscoveryIosFilterPresentation) -> Unit)?> { null }

/** Installs the application's iOS view factory. Feature UI still owns state and events. */
@Composable
fun ProvideIosDiscoveryFilterSheet(
    presenter: @Composable (DiscoveryIosFilterPresentation) -> Unit,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(LocalIosFilterPresenter provides presenter, content = content)

class DiscoveryIosFilterPresentation internal constructor(
    val state: DiscoveryIosFilterViewState,
    val onReset: () -> Unit,
    val onPrice: (Int) -> Unit,
    val onToggle: (String, String) -> Unit,
    val onApply: () -> Unit,
    val onClose: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal actual fun PlatformDiscoveryFilterSheet(state: SearchState, onAction: (SearchAction) -> Unit): Boolean {
    val presenter = LocalIosFilterPresenter.current ?: return false
    val draft = remember { DiscoveryFilterDraft(state.filters) }
    val currentFilters by rememberUpdatedState(state.filters)
    val action by rememberUpdatedState(onAction)
    val close = { action(SearchAction.CloseFilters) }
    presenter(DiscoveryIosFilterPresentation(
        state = discoveryIosFilterViewState(state, draft.filters),
        onReset = draft::reset,
        onPrice = draft::price,
        onToggle = draft::toggle,
        onApply = {
            action(SearchAction.ApplyFilters(draft.applyTo(currentFilters)))
            close()
        },
        onClose = close,
        onRetry = { action(SearchAction.RetryCatalogs) },
    ))
    return true
}
