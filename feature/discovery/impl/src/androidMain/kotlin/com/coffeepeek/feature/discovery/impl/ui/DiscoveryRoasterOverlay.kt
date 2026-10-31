package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterSlots
import com.coffeepeek.feature.discovery.impl.search.SearchAction

@Composable
internal fun DiscoveryRoasterOverlay(
    ui: DiscoveryTabState, roasters: DiscoveryRoasterSlots, context: DiscoveryRoasterContext,
    reduceMotion: Boolean, onAction: (SearchAction) -> Unit,
) {
    AnimatedVisibility(
        visible = ui.showRoasters, modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(if (reduceMotion) 0 else 300)) + slideInVertically(tween(if (reduceMotion) 0 else 360)) { it / 5 },
        exit = fadeOut(tween(if (reduceMotion) 0 else 240)) + slideOutVertically(tween(if (reduceMotion) 0 else 360)) { -it / 5 },
    ) {
        roasters.search(context, { onAction(SearchAction.Query(it)) }, {
            ui.closeRoasters(onAction)
        })
    }
}
