package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * UIKit underlays clear a rectangle in the Compose canvas. Clip that clear operation
 * to the control, preserving the Compose background outside its rounded edges.
 * Use a canvas clip, not an offscreen layer: Clear must reach the interop canvas.
 */
fun Modifier.coffeePeekNativeUnderlayClip(shape: Shape): Modifier = drawWithCache {
    val path = when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Generic -> outline.path
    }
    onDrawWithContent { clipPath(path) { this@onDrawWithContent.drawContent() } }
}
