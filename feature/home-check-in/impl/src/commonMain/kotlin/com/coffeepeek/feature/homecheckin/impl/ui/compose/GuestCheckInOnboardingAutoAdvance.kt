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
internal fun GuestCheckInOnboardingAutoAdvance(pager: PagerState) {
    if (LocalInspectionMode.current) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, pager) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            advanceOnboardingPages(pager)
        }
    }
}

private suspend fun advanceOnboardingPages(pager: PagerState) = coroutineScope {
    val scrolling = snapshotFlow { pager.isScrollInProgress }
    while (isActive) {
        scrolling.first { !it }
        val userScrolled = withTimeoutOrNull(2_000) { scrolling.first { it } } != null
        if (userScrolled || pager.isScrollInProgress) continue

        // A gesture can interrupt this animation without cancelling the autoplay loop.
        launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(420)) }.join()
    }
}
