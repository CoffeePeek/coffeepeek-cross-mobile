package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
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
 * The cut card lifts off the front where the bag lies in the [photo] (its bounds in this overlay), straightens, comes to
 * the middle and then flies up off the screen; [onDone] once it is gone. [aspect] is the card's width over height.
 */
@Composable
internal fun ShelfCardPeel(card: ByteArray, aspect: Float, frame: BagScanFrame?, photo: Rect, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val bitmap = rememberShelfBitmap(card)
    val done by rememberUpdatedState(onDone)
    val progress = remember(card) { Animatable(0f) }
    val flight = remember(card) { Animatable(0f) }
    LaunchedEffect(card, bitmap != null) {
        if (bitmap == null) return@LaunchedEffect
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(250)
        flight.animateTo(1f, tween(380, easing = Easing { it * it }))
        done()
    }
    val image = bitmap ?: return
    val t = progress.value
    val rect = lerp(frame?.bounds(photo) ?: centred(photo, aspect, 0.4f), centred(photo, aspect, 0.78f), t)
    val corner = with(LocalDensity.current) { 18.dp.toPx() }
    Canvas(modifier.fillMaxSize()) {
        drawRoundRect(Color.Black.copy(alpha = 0.35f * t), photo.topLeft, photo.size, CornerRadius(corner))
    }
    with(LocalDensity.current) {
        val away = rect.bottom + 120.dp.toPx()
        Image(image, null, Modifier.offset { IntOffset(rect.left.roundToInt(), (rect.top - flight.value * away).roundToInt()) }
            .size(rect.width.toDp(), rect.height.toDp())
            .graphicsLayer {
                rotationZ = -4f * sin(PI * t).toFloat() + 6f * flight.value
                shadowElevation = 18.dp.toPx() * minOf(1f, t * 3)
                shape = ShelfCardShape
                clip = true
            }, contentScale = ContentScale.FillBounds)
    }
}

private fun BagScanFrame.bounds(photo: Rect): Rect {
    val xs = points.map { photo.left + it.first * photo.width }
    val ys = points.map { photo.top + it.second * photo.height }
    return Rect(xs.min(), ys.min(), xs.max(), ys.max())
}

/** A rect of [aspect] as large as [share] of [area] allows, in its middle. */
private fun centred(area: Rect, aspect: Float, share: Float): Rect {
    val width = minOf(area.width * share, area.height * share * aspect)
    val height = width / aspect
    return Rect(Offset(area.center.x - width / 2, area.center.y - height / 2), Size(width, height))
}

/** The card dropping from above the screen into [slot] (overlay coordinates) and springing there; [onLanded] after. */
@Composable
internal fun ShelfCardLanding(card: ByteArray, slot: Rect, onLanded: () -> Unit, modifier: Modifier = Modifier) {
    val landing = rememberShelfLanding(card, landing = true, onLanded)
    val image = rememberShelfBitmap(card) ?: return
    with(LocalDensity.current) {
        Image(image, null, modifier.offset { IntOffset(slot.left.roundToInt(), slot.top.roundToInt()) }
            .size(slot.width.toDp(), slot.height.toDp())
            .shelfLanding(landing, fallHeight = slot.bottom + 40.dp.toPx())
            .shadow(10.dp, ShelfCardShape).clip(ShelfCardShape), contentScale = ContentScale.FillBounds)
    }
}
