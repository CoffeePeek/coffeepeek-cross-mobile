package com.coffeepeek.admin.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal val PullRefreshSettleSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow,
)

@Composable
fun CoffeePeekPullToRefresh(
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (scrollModifier: Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }
    val indicatorSize = CpDimens.loaderButton
    val indicatorSizePx = with(density) { indicatorSize.toPx() }
    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastRefreshMark by remember { mutableStateOf<TimeMark?>(null) }

    val isRefreshingState by rememberUpdatedState(isRefreshing)
    val listStateState by rememberUpdatedState(listState)
    val onRefreshState by rememberUpdatedState(onRefresh)

    val nestedScrollConnection = remember(thresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val currentListState = listStateState
                val atTop = currentListState.firstVisibleItemIndex == 0 &&
                    currentListState.firstVisibleItemScrollOffset == 0

                if (!atTop) {
                    if (pullOffset > 0f) pullOffset = 0f
                    return Offset.Zero
                }
                if (available.y > 0f && !isRefreshingState) {
                    isDragging = true
                    val damped = available.y * 0.5f
                    val next = (pullOffset + damped).coerceAtMost(thresholdPx * 1.4f)
                    val consumedY = (next - pullOffset) / 0.5f
                    pullOffset = next
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && !isRefreshingState && available.y < 0f && pullOffset > 0f) {
                    isDragging = true
                    val release = min(-available.y, pullOffset / 0.5f)
                    pullOffset = (pullOffset - release * 0.5f).coerceAtLeast(0f)
                    return Offset(0f, -release)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val wasPulling = pullOffset > 0f
                isDragging = false
                val refreshAllowed = lastRefreshMark?.elapsedNow()?.let { it > 1.seconds } ?: true
                if (
                    pullOffset >= thresholdPx &&
                    !isRefreshingState &&
                    refreshAllowed
                ) {
                    lastRefreshMark = TimeSource.Monotonic.markNow()
                    onRefreshState()
                }
                pullOffset = 0f
                return if (wasPulling) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullOffset = 0f
    }

    val contentOffsetPx by animateFloatAsState(
        targetValue = if (isRefreshing) thresholdPx else pullOffset,
        animationSpec = if (isDragging) snap() else PullRefreshSettleSpec,
        label = "pull-refresh-offset",
    )
    val scrollModifier = Modifier
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer { translationY = contentOffsetPx }
    val showIndicator = isRefreshing || contentOffsetPx > 0.5f

    Box(modifier = modifier) {
        content(scrollModifier)

        if (showIndicator) {
            val progress = (contentOffsetPx / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = progress
                        translationY = ((contentOffsetPx - indicatorSizePx) / 2f).coerceAtLeast(0f)
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                CoffeePeekLoader(
                    size = indicatorSize,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}
