package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.impl.tab.*
import kotlin.math.roundToInt

internal data class DiscoveryMapPresentation(val canvasSize: DpSize, val expanded: Boolean, val controlsExpanded: Boolean)

@Composable
internal fun DiscoveryMapLayer(
    ui: DiscoveryTabState, motion: DiscoveryTabMotion, host: DiscoveryHost, canvasSize: DpSize,
    onToggleExpand: () -> Unit, map: @Composable (DiscoveryMapPresentation, () -> Unit, Modifier) -> Unit,
) {
    val visible by remember(motion) { derivedStateOf { motion.discoveryProgress.value > 0f } }
    val controlsExpanded by remember(ui, motion) { derivedStateOf { ui.mapExpanded && motion.expansion.value == 1f } }
    host.retained(visible) {
        Box(Modifier.discoveryMapViewport(ui, motion)) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                translationY = discoveryMapOffsetY(ui.viewportHeight, motion.discoveryProgress.value)
            }) {
                map(DiscoveryMapPresentation(canvasSize, ui.mapExpanded, controlsExpanded), onToggleExpand,
                    Modifier.discoveryMapFrame(ui, motion, canvasSize))
            }
        }
    }
}

private fun Modifier.discoveryMapViewport(ui: DiscoveryTabState, motion: DiscoveryTabMotion): Modifier =
    fillMaxSize().layout { measurable, constraints ->
        val clipTop = mapClipTop(ui.viewportTop, motion.expansion.value, constraints.maxHeight)
        val height = constraints.maxHeight - clipTop
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, clipTop) }
    }.clipToBounds()

@Composable
private fun Modifier.discoveryMapFrame(ui: DiscoveryTabState, motion: DiscoveryTabMotion, canvasSize: DpSize): Modifier {
    val density = LocalDensity.current
    val width = with(density) { canvasSize.width.toPx() }
    val height = with(density) { canvasSize.height.toPx() }
    val radius = with(density) { CoffeePeekDimens.radius2xl.toPx() }
    val outline = MaterialTheme.colorScheme.outline
    val stroke = with(density) { 1.dp.toPx() }
    return offset {
        val bounds = expandedFrame(ui.previewBounds, width, height, motion.expansion.value)
        val clipTop = mapClipTop(ui.viewportTop, motion.expansion.value, height.roundToInt())
        IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt() - clipTop)
    }.layout { measurable, _ ->
        val bounds = expandedFrame(ui.previewBounds, width, height, motion.expansion.value)
        val placeable = measurable.measure(Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1)))
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }.graphicsLayer {
        clip = true
        shape = RoundedCornerShape(mapCornerRadius(radius, motion.expansion.value))
    }.drawWithContent {
        drawContent()
        drawRoundRect(outline, cornerRadius = CornerRadius(mapCornerRadius(radius, motion.expansion.value)), style = Stroke(stroke))
    }
}
