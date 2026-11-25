package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Artwork decoding only: native controls do not retain a Painter or composition. */
internal fun coffeePeekVectorBitmap(vector: ImageVector, side: Int, density: Density, direction: LayoutDirection): ImageBitmap {
    require(vector.viewportWidth > 0 && vector.viewportHeight > 0 && side > 0)
    val bitmap = ImageBitmap(side, side)
    CanvasDrawScope().draw(density, direction, Canvas(bitmap), Size(side.toFloat(), side.toFloat())) {
        withTransform({
            if (vector.autoMirror && direction == LayoutDirection.Rtl) scale(-1f, 1f)
            scale(side / vector.viewportWidth, side / vector.viewportHeight, Offset.Zero)
        }) { coffeePeekDrawVectorGroup(vector.root) }
    }
    return bitmap
}

private fun DrawScope.coffeePeekDrawVectorGroup(group: VectorGroup) {
    withTransform({
        translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        rotate(group.rotation, Offset.Zero)
        scale(group.scaleX, group.scaleY, Offset.Zero)
        translate(-group.pivotX, -group.pivotY)
    }) {
        if (group.clipPathData.isEmpty()) coffeePeekDrawVectorChildren(group)
        else clipPath(group.clipPathData.toPath()) { coffeePeekDrawVectorChildren(group) }
    }
}

private fun DrawScope.coffeePeekDrawVectorChildren(group: VectorGroup) {
    group.forEach { node -> when (node) {
        is VectorGroup -> coffeePeekDrawVectorGroup(node)
        is VectorPath -> coffeePeekDrawVectorPath(node)
    } }
}

private fun DrawScope.coffeePeekDrawVectorPath(node: VectorPath) {
    val path = coffeePeekVectorPath(node)
    node.fill?.let { drawPath(path, it, node.fillAlpha) }
    node.stroke?.let { drawPath(path, it, node.strokeAlpha,
        style = Stroke(node.strokeLineWidth, node.strokeLineMiter, node.strokeLineCap, node.strokeLineJoin)) }
}

private fun coffeePeekVectorPath(node: VectorPath): Path {
    val source = node.pathData.toPath().apply { fillType = node.pathFillType }
    if (node.trimPathStart == 0f && node.trimPathEnd == 1f) return source
    val measure = PathMeasure().apply { setPath(source, false) }
    val start = (node.trimPathStart + node.trimPathOffset) % 1f * measure.length
    val end = (node.trimPathEnd + node.trimPathOffset) % 1f * measure.length
    return Path().apply {
        fillType = node.pathFillType
        if (start > end) {
            measure.getSegment(start, measure.length, this, true)
            measure.getSegment(0f, end, this, true)
        } else measure.getSegment(start, end, this, true)
    }
}
