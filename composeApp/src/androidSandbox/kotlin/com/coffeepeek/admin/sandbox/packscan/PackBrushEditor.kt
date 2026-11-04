package com.coffeepeek.admin.sandbox.packscan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coffeepeek.R
import com.coffeepeek.admin.sandbox.packscan.geometry.PackPoint
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlin.math.min

/** Bag image with a finger brush; strokes are stored in image pixels so they map 1:1 onto the repaint mask. */
@Composable internal fun PackBrushEditor(
    image: Bitmap,
    strokes: SnapshotStateList<List<PackPoint>>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val picture = remember(image) { image.asImageBitmap() }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val fit = remember(box, image) { PackFit(box, image.width, image.height) }
    val label = stringResource(R.string.sandbox_pack_probe_editor_description)
    Box(
        modifier.fillMaxWidth().height(480.dp).checkerboard().onSizeChanged { box = it }
            .pointerInput(enabled, fit) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { strokes.add(listOf(fit.toImage(it))) },
                    onDrag = { change, _ ->
                        change.consume()
                        strokes[strokes.lastIndex] = strokes.last() + fit.toImage(change.position)
                    },
                )
            },
    ) {
        Image(picture, label, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.matchParentSize()) { drawStrokes(strokes, fit, image.brushRadius() * fit.scale) }
    }
}

private fun DrawScope.drawStrokes(strokes: List<List<PackPoint>>, fit: PackFit, radius: Float) {
    for (stroke in strokes.filter { it.isNotEmpty() }) {
        val path = Path()
        stroke.forEachIndexed { index, point ->
            val offset = fit.toScreen(point)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(path, BrushColor, style = Stroke(width = radius * 2, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (stroke.size == 1) drawCircle(BrushColor, radius, fit.toScreen(stroke.first()))
    }
}

// Same letterbox as ContentScale.Fit, centred in the editor box.
private class PackFit(box: IntSize, width: Int, height: Int) {
    val scale = if (width == 0 || height == 0) 1f else min(box.width / width.toFloat(), box.height / height.toFloat())
    private val left = (box.width - width * scale) / 2
    private val top = (box.height - height * scale) / 2

    fun toImage(offset: Offset) = PackPoint((offset.x - left) / scale, (offset.y - top) / scale)

    fun toScreen(point: PackPoint) = Offset(left + point.x * scale, top + point.y * scale)
}

// Translucent so the defect under the stroke stays visible in both themes.
private val BrushColor = Color(0x99FF2D55)

@PreviewLightDark @Composable private fun PackBrushEditorPreview() = CoffeePeekTheme {
    val image = remember { Bitmap.createBitmap(300, 420, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.YELLOW) } }
    val strokes = remember { mutableStateListOf(listOf(PackPoint(40f, 60f), PackPoint(120f, 90f)), listOf(PackPoint(200f, 300f))) }
    Surface { PackBrushEditor(image, strokes, enabled = true) }
}
