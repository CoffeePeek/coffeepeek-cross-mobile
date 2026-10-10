package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpColor
import com.coffeepeek.core.designsystem.theme.CpDimens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val ROTATION_DURATION_MS = 1600
private const val FILL_DURATION_MS = 2800
private const val MORPH_DURATION_MS = 5000
private const val MAX_FILL_FRACTION = 0.72f

@Composable
fun CoffeePeekLoader(
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = CpDimens.loaderDefault,
    color: Color = CpColor.Primary,
    strokeWidth: Dp = (size / 16f).coerceAtLeast(2.dp),
) {
    val transition = rememberInfiniteTransition(label = "coffeepeek-loader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ROTATION_DURATION_MS, easing = LinearEasing),
        ),
        label = "coffeepeek-loader-spin",
    )
    val fillProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FILL_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "coffeepeek-loader-fill",
    )
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = MORPH_DURATION_MS
                0f at 0 using FastOutSlowInEasing
                1f at 1250 using FastOutSlowInEasing
                2f at 2500 using FastOutSlowInEasing
                3f at 3750 using FastOutSlowInEasing
                4f at 5000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "coffeepeek-loader-morph",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        val strokePx = strokeWidth.toPx()
        val inset = strokePx / 2f
        val ringSize = Size(this.size.width - strokePx, this.size.height - strokePx)
        val wobbleX = 1f + 0.09f * kotlin.math.sin(morph * PI).toFloat()
        val wobbleY = 1f + 0.08f * kotlin.math.cos(morph * PI * 0.75f).toFloat()
        val corner = ringSize.minDimension * (0.32f + 0.08f * kotlin.math.sin(morph * PI * 0.5f).toFloat())
        val roundRect = RoundRect(
            rect = Rect(Offset(inset, inset), ringSize),
            cornerRadius = CornerRadius(corner, corner * 0.86f),
        )
        val trackPath = Path().apply { addRoundRect(roundRect) }
        val perimeter = sampleRoundRectPerimeter(roundRect)
        val trackStroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val fillStroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Round,
        )

        rotate(rotation) {
            scale(wobbleX, wobbleY) {
                drawPath(
                    path = trackPath,
                    color = color.copy(alpha = 0.18f),
                    style = trackStroke,
                )

                val fillEnd = (fillProgress * MAX_FILL_FRACTION).coerceIn(0f, MAX_FILL_FRACTION)
                if (fillEnd > 0.01f) {
                    val fillPath = pathAlongPerimeter(perimeter, endFraction = fillEnd)
                    drawPath(
                        path = fillPath,
                        color = color,
                        style = fillStroke,
                    )
                }
            }
        }
    }
}

private fun sampleRoundRectPerimeter(roundRect: RoundRect, edgeSteps: Int = 6, cornerSteps: Int = 10): List<Offset> {
    val points = mutableListOf<Offset>()
    val l = roundRect.left
    val t = roundRect.top
    val r = roundRect.right
    val b = roundRect.bottom

    fun cornerRadii(which: String): Pair<Float, Float> = when (which) {
        "tl" -> roundRect.topLeftCornerRadius.x to roundRect.topLeftCornerRadius.y
        "tr" -> roundRect.topRightCornerRadius.x to roundRect.topRightCornerRadius.y
        "br" -> roundRect.bottomRightCornerRadius.x to roundRect.bottomRightCornerRadius.y
        else -> roundRect.bottomLeftCornerRadius.x to roundRect.bottomLeftCornerRadius.y
    }

    fun addLine(x0: Float, y0: Float, x1: Float, y1: Float, steps: Int) {
        if (steps <= 0) return
        for (i in 0 until steps) {
            val fraction = i / steps.toFloat()
            points.add(Offset(x0 + (x1 - x0) * fraction, y0 + (y1 - y0) * fraction))
        }
    }

    fun addArc(cx: Float, cy: Float, startAngle: Float, sweep: Float, steps: Int, rx: Float, ry: Float) {
        if (steps <= 0) return
        for (i in 0 until steps) {
            val fraction = i / steps.toFloat()
            val angle = (startAngle + sweep * fraction) * PI.toFloat() / 180f
            points.add(Offset(cx + rx * cos(angle), cy + ry * sin(angle)))
        }
    }

    val (trx, tryRadius) = cornerRadii("tr")
    val (brx, bry) = cornerRadii("br")
    val (blx, bly) = cornerRadii("bl")
    val (tlx, tly) = cornerRadii("tl")

    addLine(l + tlx, t, r - trx, t, edgeSteps)
    addArc(r - trx, t + tryRadius, -90f, 90f, cornerSteps, trx, tryRadius)
    addLine(r, t + tryRadius, r, b - bry, edgeSteps)
    addArc(r - brx, b - bry, 0f, 90f, cornerSteps, brx, bry)
    addLine(r - brx, b, l + blx, b, edgeSteps)
    addArc(l + blx, b - bly, 90f, 90f, cornerSteps, blx, bly)
    addLine(l, b - bly, l, t + tly, edgeSteps)
    addArc(l + tlx, t + tly, 180f, 90f, cornerSteps, tlx, tly)

    return points
}

private fun pathAlongPerimeter(points: List<Offset>, endFraction: Float): Path {
    val path = Path()
    if (points.isEmpty() || endFraction <= 0f) return path

    val lastIndex = points.lastIndex.toFloat()
    val endPosition = (endFraction * lastIndex).coerceIn(1f, lastIndex)
    val endIdx = endPosition.toInt()
    val endPartial = endPosition - endIdx

    path.moveTo(points[0].x, points[0].y)
    for (i in 1..endIdx) {
        path.lineTo(points[i].x, points[i].y)
    }
    if (endPartial > 0f && endIdx < points.lastIndex) {
        val from = points[endIdx]
        val to = points[endIdx + 1]
        path.lineTo(
            from.x + (to.x - from.x) * endPartial,
            from.y + (to.y - from.y) * endPartial,
        )
    }
    return path
}

@Composable
private fun CoffeePeekLoaderPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface { CoffeePeekLoader("Loading") }
}

@Preview @Composable private fun CoffeePeekLoaderLightPreview() = CoffeePeekLoaderPreviewContent(false)
@Preview @Composable private fun CoffeePeekLoaderDarkPreview() = CoffeePeekLoaderPreviewContent(true)
