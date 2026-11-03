package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Measures against at least the resting maximum to avoid repeated detent shrinkage.
 * UIKit may expand the viewport for the keyboard; the body fills that extra space,
 * while its reported height stays capped so keyboard animation does not change the detent.
 * Constraints stay finite so lazy lists and scroll containers remain supported.
 */
@Composable
fun CoffeePeekContentSizedSheetLayout(
    maxContentHeight: Dp,
    onContentHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maximumHeight = with(LocalDensity.current) { maxContentHeight.roundToPx().coerceAtLeast(1) }
    val onHeightChanged = rememberUpdatedState(onContentHeightChanged)
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Column(Modifier.fillMaxWidth().onSizeChanged { onHeightChanged.value(it.height.coerceAtMost(maximumHeight)) }) {
                val fittingScope = remember(this) { CoffeePeekContentSizedColumnScope(this) }
                content(fittingScope)
            }
        },
    ) { measurables, constraints ->
        val measurementHeight = if (constraints.hasBoundedHeight) {
            maxOf(maximumHeight, constraints.maxHeight)
        } else maximumHeight
        val body = measurables.single().measure(
            constraints.copy(minHeight = 0, maxHeight = measurementHeight),
        )
        layout(constraints.constrainWidth(body.width), constraints.constrainHeight(body.height)) {
            body.placeRelative(0, 0)
        }
    }
}

/** Top-level weights share the available space without stretching short sheet bodies. */
private class CoffeePeekContentSizedColumnScope(private val column: ColumnScope) : ColumnScope by column {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = with(column) {
        this@weight.weight(weight, fill = false)
    }
}

/** Positive provisional height mounts content before its first measurement. */
fun coffeePeekNativeSheetDetentHeight(
    contentHeight: Double, maximumHeight: Double, heightFraction: Float, expandToMaxHeight: Boolean = false,
): Double {
    val limit = (maximumHeight * heightFraction.coerceIn(0.1f, 1f)).coerceAtLeast(1.0)
    return when {
        expandToMaxHeight -> limit
        contentHeight > 0.0 -> contentHeight.coerceAtMost(limit)
        else -> limit / 2
    }
}

@Composable
private fun CoffeePeekContentSizedSheetLayoutPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekContentSizedSheetLayout(300.dp, {}) { Text("Sheet content") }
}

@Preview @Composable private fun CoffeePeekContentSizedSheetLayoutLightPreview() = CoffeePeekContentSizedSheetLayoutPreview(false)
@Preview @Composable private fun CoffeePeekContentSizedSheetLayoutDarkPreview() = CoffeePeekContentSizedSheetLayoutPreview(true)
