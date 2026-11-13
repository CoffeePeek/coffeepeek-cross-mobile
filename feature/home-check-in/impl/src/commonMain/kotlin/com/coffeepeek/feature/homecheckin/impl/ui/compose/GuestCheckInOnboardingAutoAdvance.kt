package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun GuestCheckInOnboardingAutoAdvance(pager: PagerState, held: Boolean = false) {
    if (LocalInspectionMode.current) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Restarting on [held] both freezes the countdown mid-hold and starts it over on release,
    // like a story: the slide never advances while the finger is down.
    LaunchedEffect(lifecycle, pager, held) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            advanceOnboardingPages(pager, held)
        }
    }
}

private suspend fun advanceOnboardingPages(pager: PagerState, held: Boolean) = coroutineScope {
    val scrolling = snapshotFlow { pager.isScrollInProgress }
    val paused = snapshotFlow { held || pager.isScrollInProgress }
    while (isActive) {
        if (pager.currentPage >= pager.pageCount - 1) return@coroutineScope
        paused.first { !it }
        val userScrolled = withTimeoutOrNull(2_000) { scrolling.first { it } } != null
        if (userScrolled || pager.isScrollInProgress) continue

        // A gesture can interrupt this animation without cancelling the autoplay loop.
        launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(420)) }.join()
    }
}
