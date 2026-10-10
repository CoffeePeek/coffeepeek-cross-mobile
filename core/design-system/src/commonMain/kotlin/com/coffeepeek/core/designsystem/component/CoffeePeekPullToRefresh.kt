package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Attach the supplied modifier to the LazyColumn using [listState].
 * The caller owns refresh work, errors and [isRefreshing]; there are no requests here.
 */
@Composable
fun CoffeePeekPullToRefresh(
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    refreshDescription: String,
    loadingDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    threshold: Dp = 72.dp,
    cooldown: Duration = 1.seconds,
    content: @Composable (scrollModifier: Modifier) -> Unit,
) {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val gesture = remember(thresholdPx, cooldown) {
        val origin = TimeSource.Monotonic.markNow()
        PullRefreshGestureState(thresholdPx, cooldown) { origin.elapsedNow() }
    }
    val refreshingNow by rememberUpdatedState(isRefreshing)
    val enabledNow by rememberUpdatedState(enabled)
    val listNow by rememberUpdatedState(listState)
    val refreshNow by rememberUpdatedState(onRefresh)
    fun atTop() = listNow.firstVisibleItemIndex == 0 && listNow.firstVisibleItemScrollOffset == 0
    fun allowed() = enabledNow && !refreshingNow

    val connection = remember(gesture) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return Offset(0f, gesture.pull(available.y, atTop(), allowed()))
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (!allowed()) {
                    gesture.reset()
                    return Offset.Zero
                }
                return Offset(0f, gesture.retract(available.y))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val hadPull = gesture.offset > 0f
                val trigger = gesture.release(allowed(), atTop())
                if (trigger) refreshNow()
                // Never steal horizontal or ordinary list-fling velocity.
                return if (hadPull) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing, enabled) { gesture.reset() }
    val offset = if (isRefreshing) thresholdPx else if (enabled) gesture.offset else 0f
    Box(modifier.semantics {
        if (enabled && !isRefreshing) {
            customActions = listOf(CustomAccessibilityAction(refreshDescription) {
                if (gesture.request(allowed())) {
                    refreshNow()
                    true
                } else false
            })
        }
    }) {
        content(Modifier.nestedScroll(connection).graphicsLayer { translationY = offset })
        if (isRefreshing || offset > 0f) {
            val progress = (offset / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = CpDimens.spacing2)
                    .graphicsLayer { alpha = if (isRefreshing) 1f else progress }
                    .clearAndSetSemantics {
                        contentDescription = if (isRefreshing) loadingDescription else refreshDescription
                        progressBarRangeInfo = if (isRefreshing) ProgressBarRangeInfo.Indeterminate
                            else ProgressBarRangeInfo(progress, 0f..1f)
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                CoffeePeekLoader(loadingDescription, size = CpDimens.loaderButton, strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun CoffeePeekPullToRefreshPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var refreshing by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    Surface {
        Column(Modifier.padding(16.dp)) {
            CoffeePeekPullToRefresh(list, refreshing, { refreshing = true }, "Refresh", "Refreshing",
                modifier = Modifier.height(240.dp)) { scrollModifier ->
                LazyColumn(state = list, modifier = scrollModifier.fillMaxSize()) {
                    items(12) { Text("Example item ${it + 1}", Modifier.fillMaxWidth().padding(16.dp)) }
                }
            }
            AppButton("Finish sample refresh", { refreshing = false }, enabled = refreshing)
        }
    }
}

@Composable
private fun CoffeePeekRefreshingPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    val list = rememberLazyListState()
    Surface {
        CoffeePeekPullToRefresh(list, true, {}, "Refresh", "Refreshing",
            modifier = Modifier.height(240.dp).padding(16.dp)) { scrollModifier ->
            LazyColumn(state = list, modifier = scrollModifier.fillMaxSize()) {
                items(4) { Text("Existing item ${it + 1}", Modifier.padding(16.dp)) }
            }
        }
    }
}

@Preview @Composable private fun CoffeePeekPullToRefreshLightPreview() = CoffeePeekPullToRefreshPreviewContent(false)
@Preview @Composable private fun CoffeePeekPullToRefreshDarkPreview() = CoffeePeekPullToRefreshPreviewContent(true)
@Preview @Composable private fun CoffeePeekRefreshingLightPreview() = CoffeePeekRefreshingPreviewContent(false)
@Preview @Composable private fun CoffeePeekRefreshingDarkPreview() = CoffeePeekRefreshingPreviewContent(true)
