package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.shelf.domain.model.BagScanFrame
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** A soft yellow band running down the photo while the scanner looks for the bag. */
@Composable
internal fun ShelfScanSweep(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val position by rememberInfiniteTransition().animateFloat(-0.2f, 1.2f,
        infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart))
    Canvas(modifier.fillMaxSize()) {
        val band = size.height * 0.22f
        val center = position * size.height
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.32f), Color.Transparent),
            startY = center - band, endY = center + band))
        drawLine(color.copy(alpha = 0.8f), Offset(0f, center), Offset(size.width, center), strokeWidth = 2.dp.toPx())
    }
}

/** The found front: the photo around it dims while its outline is drawn corner to corner. */
@Composable
internal fun ShelfFrameReveal(frame: BagScanFrame, modifier: Modifier = Modifier, dim: Float = 0.45f) {
    val color = MaterialTheme.colorScheme.primary
    val progress = remember(frame) { Animatable(0f) }
    LaunchedEffect(frame) { progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val quad = frame.path(size)
        val outside = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addPath(quad)
        }
        drawPath(outside, Color.Black.copy(alpha = dim * progress.value))
        // A partial outline is an open polyline that starts and ends in the first corner: round its ends while it grows,
        // and draw the closed quad once complete, so that corner is joined like the other three.
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (progress.value >= 1f) drawPath(quad, color, style = stroke)
        else {
            val measure = PathMeasure().apply { setPath(quad, true) }
            drawPath(Path().also { measure.getSegment(0f, measure.length * progress.value, it, true) }, color, style = stroke)
        }
    }
}

/**
 * The cut card lifts off the front where the bag lies in the photo, straightens and comes to the middle; [onDone] once it
 * has settled. [aspect] is the card's width over height.
 */
@Composable
internal fun ShelfCardPeel(card: ByteArray, aspect: Float, frame: BagScanFrame?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val bitmap = rememberShelfBitmap(card)
    val done by rememberUpdatedState(onDone)
    val progress = remember(card) { Animatable(0f) }
    LaunchedEffect(card, bitmap != null) {
        if (bitmap == null) return@LaunchedEffect
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(350)
        done()
    }
    val image = bitmap ?: return
    BoxWithConstraints(modifier.fillMaxSize()) {
        val area = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val start = frame?.bounds(area) ?: centred(area, aspect, 0.4f)
        val rect = lerp(start, centred(area, aspect, 0.78f), progress.value)
        val t = progress.value
        Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black.copy(alpha = 0.35f * t)) }
        with(LocalDensity.current) {
            Image(image, null, Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .size(rect.width.toDp(), rect.height.toDp())
                .graphicsLayer {
                    rotationZ = -4f * sin(PI * t).toFloat()
                    shadowElevation = 18.dp.toPx() * minOf(1f, t * 3)
                    shape = ShelfCardShape
                    clip = true
                }, contentScale = ContentScale.FillBounds)
        }
    }
}

private fun BagScanFrame.bounds(area: Size): Rect {
    val xs = points.map { it.first * area.width }
    val ys = points.map { it.second * area.height }
    return Rect(xs.min(), ys.min(), xs.max(), ys.max())
}

/** A rect of [aspect] as large as [share] of [area] allows, in its middle. */
private fun centred(area: Size, aspect: Float, share: Float): Rect {
    val width = minOf(area.width * share, area.height * share * aspect)
    val height = width / aspect
    return Rect(Offset((area.width - width) / 2, (area.height - height) / 2), Size(width, height))
}
