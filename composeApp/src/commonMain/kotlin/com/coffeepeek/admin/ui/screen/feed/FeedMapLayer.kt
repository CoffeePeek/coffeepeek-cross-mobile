package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
internal fun FeedMapLayer(layout: FeedLayoutState, motion: FeedMapMotion, screenSize: Size, canvasSize: DpSize,
    mapPreview: @Composable (Boolean, () -> Unit, DpSize, Modifier) -> Unit) {
    if (layout.mapAnchor.width <= 0f && !layout.mapExpanded) return
    val visible by remember(motion) { derivedStateOf { motion.progress.value > 0f } }
    val controlsExpanded by remember(layout, motion) { derivedStateOf { layout.mapExpanded && motion.expansion.value == 1f } }
    val mapModifier = feedMapBoundsModifier(layout, motion, screenSize)
    RetainedContent(visible) {
        Box(Modifier.fillMaxSize().feedMapViewport(layout, motion).clipToBounds()) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                clip = true
                translationY = discoveryMapOffsetY(layout.viewportHeight, motion.progress.value)
            }) {
                Box(Modifier.fillMaxSize()) {
                    mapPreview(controlsExpanded, { layout.mapExpanded = !layout.mapExpanded }, canvasSize, mapModifier)
                }
            }
        }
    }
}

private fun Modifier.feedMapViewport(state: FeedLayoutState, motion: FeedMapMotion): Modifier = layout { measurable, constraints ->
    val clipTop = (state.viewportTop * (1f - motion.expansion.value)).roundToInt().coerceIn(0, constraints.maxHeight)
    val height = constraints.maxHeight - clipTop
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, clipTop) }
}

@Composable
private fun feedMapBoundsModifier(state: FeedLayoutState, motion: FeedMapMotion, screenSize: Size): Modifier {
    val radius = with(LocalDensity.current) { CpDimens.radius2xl.toPx() }
    val outline = MaterialTheme.colorScheme.outline
    return Modifier.offset {
        val bounds = expandedMapBounds(state.previewBounds, screenSize, motion.expansion.value)
        val clipTop = (state.viewportTop * (1f - motion.expansion.value)).roundToInt().coerceIn(0, screenSize.height.roundToInt())
        IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt() - clipTop)
    }.layout { measurable, _ ->
        val bounds = expandedMapBounds(state.previewBounds, screenSize, motion.expansion.value)
        val placeable = measurable.measure(Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1)))
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }.graphicsLayer {
        clip = true
        shape = RoundedCornerShape(radius * (1f - motion.expansion.value))
    }.drawWithContent {
        drawContent()
        drawRoundRect(outline, cornerRadius = CornerRadius(radius * (1f - motion.expansion.value)), style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun FeedMapLayerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val layout = rememberFeedLayout().apply { mapExpanded = true }
    Box(Modifier.size(390.dp, 600.dp)) {
        FeedMapLayer(layout, FeedMapMotion(rememberUpdatedState(1f), rememberUpdatedState(1f)),
            Size(390f, 600f), DpSize(390.dp, 600.dp)) { _, _, _, modifier ->
            Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {}
        }
    }
}

@Preview @Composable private fun FeedMapLayerLightPreview() = FeedMapLayerPreview(false)
@Preview @Composable private fun FeedMapLayerDarkPreview() = FeedMapLayerPreview(true)
