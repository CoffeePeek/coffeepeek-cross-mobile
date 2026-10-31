package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterSlots
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

@Composable
internal fun DiscoverySearchLayer(
    state: SearchState, ui: DiscoveryTabState, motion: DiscoveryTabMotion, host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots, context: DiscoveryRoasterContext, bottomClearance: Dp,
    onAddShop: () -> Unit, onAction: (SearchAction) -> Unit,
) {
    host.retained(motion.searchOpacity.value > 0f) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = motion.searchOpacity.value
            translationY = -size.height / 5f * (1f - motion.searchOpacity.value)
        }) {
            SearchContent(state, host, onAddShop, bottomClearance + 72.dp,
                discovery = {
                    DiscoveryContent(state, host, roasters, context, ui.list, ui.shops,
                        ui.mapExpanded, motion.expansion.value, bottomClearance, onAddShop,
                        showAllRoasters = { ui.showRoasters = true }, onAction = onAction,
                        onMapSlot = { slot, ready ->
                            ui.recordMapSlot(slot, ready, motion.discoveryProgress.value, motion.expansion.value)
                        })
                },
                textResults = { roasters.preview(context) }, onAction = onAction,
                hasRoasterResults = if (state.query.isNotBlank()) roasters.hasResults(context) else null,
                onAddRoaster = roasters.onAdd,
                resultsState = ui.results,
                headerModifier = Modifier
                    .then(if (ui.mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
                    .graphicsLayer {
                        alpha = 1f - motion.expansion.value
                        translationY = -size.height * motion.expansion.value
                    }.statusBarsPadding(),
                bodyModifier = Modifier.onGloballyPositioned(ui::recordViewport), prefetch = !ui.mapExpanded,
            )
        }
    }
}
