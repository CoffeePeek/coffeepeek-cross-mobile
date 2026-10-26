package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

@Composable
internal fun DiscoveryTabContent(
    state: SearchState, host: DiscoveryHost, roasters: DiscoveryRoasterSlots,
    analytics: DiscoveryAnalytics, callbacks: TabCallbacks, bottomClearance: Dp,
    focus: MapFocus?, onAction: (SearchAction) -> Unit, modifier: Modifier = Modifier,
    map: @Composable (DiscoveryMapPresentation, () -> Unit, Modifier) -> Unit,
) {
    val ui = rememberDiscoveryTabState()
    val reduceMotion = host.reduceMotion()
    val motion = discoveryTabMotion(ui, state.showsDiscovery, reduceMotion)
    val toggleMap = discoveryMapToggle(ui, analytics)
    val context = DiscoveryRoasterContext(state.query, state.filters.roasterIds, state.filters.favoritesOnly)
    DiscoveryTabEffects(ui, state.showsDiscovery, host, analytics, callbacks, focus, onAction)
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { ui.rootOrigin = it.positionInRoot() }) {
        val canvasSize = DpSize(maxWidth, maxHeight)
        DiscoverySearchLayer(state, ui, motion, host, roasters, context, bottomClearance, callbacks.onAddShop, onAction)
        DiscoveryRoasterOverlay(ui, roasters, context, reduceMotion, onAction)
        if (ui.mapAnchor.width > 0f) DiscoveryMapLayer(ui, motion, host, canvasSize, toggleMap, map)
    }
}

@Composable
private fun DiscoveryTabContentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DiscoveryTabContent(
        state = SearchState(isLoading = false, shops = listOf(ShopSummary("sandbox-cafe", "Sandbox Cafe"))),
        host = DiscoveryHost(),
        roasters = DiscoveryRoasterSlots(preview = {}, search = { _, _, _ -> }),
        analytics = NoDiscoveryAnalytics, callbacks = TabCallbacks({}, {}, {}, {}, {}),
        bottomClearance = 80.dp, focus = null, onAction = {},
    ) { _, _, modifier -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) }
}

@Preview @Composable private fun DiscoveryTabContentPreviewLight() = DiscoveryTabContentPreview(false)
@Preview @Composable private fun DiscoveryTabContentPreviewDark() = DiscoveryTabContentPreview(true)
