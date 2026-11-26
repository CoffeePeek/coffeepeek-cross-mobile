package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_loading
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview


// Five reference contours, sampled clockwise from the top at equal angles.
private val loaderContours = CoffeePeekLoaderGeometry.contours.map { contour ->
    contour.map { Offset(it.x, it.y) }
}

/** The original CoffeePeek contour animation, also retained by previous Compose screens. */
@Composable
fun CoffeePeekMorphingLoader(
    contentDescription: String = stringResource(Res.string.coffeepeek_loading),
    modifier: Modifier = Modifier,
    size: Dp = CoffeePeekDimens.loaderDefault,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = (size / 16f).coerceAtLeast(2.dp),
    progress: Float? = null,
) {
    val motion = rememberLoaderMotion()
    val paths = remember { CoffeePeekLoaderPaths() }
    val fraction = progress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    Canvas(modifier.size(size).semantics {
        this.contentDescription = contentDescription
        progressBarRangeInfo = fraction?.let { ProgressBarRangeInfo(it, 0f..1f) }
            ?: ProgressBarRangeInfo.Indeterminate
    }) {
        paths.prepare(center, ((this.size.minDimension - strokeWidth.toPx()) / 2f).coerceAtLeast(0f),
            motion.morph.value, fraction ?: (motion.fill.value * CoffeePeekLoaderGeometry.maxFillFraction))
        rotate(motion.rotation.value) { drawLoader(paths, color, strokeWidth.toPx()) }
    }
}

private class CoffeePeekLoaderMotion(val rotation: State<Float>, val fill: State<Float>, val morph: State<Float>)

@Composable
private fun rememberLoaderMotion(): CoffeePeekLoaderMotion {
    val transition = rememberInfiniteTransition(label = "coffeepeek-loader")
    val rotation = transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(CoffeePeekLoaderGeometry.rotationDurationMillis, easing = LinearEasing)), label = "coffeepeek-loader-spin")
    val fill = transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(CoffeePeekLoaderGeometry.fillDurationMillis, easing = LinearEasing)), label = "coffeepeek-loader-fill")
    val morph = transition.animateFloat(0f, loaderContours.size.toFloat(),
        infiniteRepeatable(keyframes {
            durationMillis = CoffeePeekLoaderGeometry.morphDurationMillis
            for (shape in 0..loaderContours.size) {
                shape.toFloat() at (shape * CoffeePeekLoaderGeometry.morphDurationMillis / loaderContours.size) using FastOutSlowInEasing
            }
        }), label = "coffeepeek-loader-morph")
    return remember(rotation, fill, morph) { CoffeePeekLoaderMotion(rotation, fill, morph) }
}

private class CoffeePeekLoaderPaths {
    val track = Path()
    val fill = Path()
    private val measure = PathMeasure()

    fun prepare(center: Offset, radius: Float, morph: Float, fillProgress: Float) {
        track.setLoaderContour(center, radius, morph)
        measure.setPath(track, forceClosed = true)
        fill.reset()
        val end = fillProgress.coerceIn(0f, 1f)
        if (end > 0.01f) measure.getSegment(0f, measure.length * end, fill)
    }
}

private fun DrawScope.drawLoader(paths: CoffeePeekLoaderPaths, color: Color, stroke: Float) {
    drawPath(paths.track, color.copy(alpha = color.alpha * 0.18f),
        style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(paths.fill, color, style = Stroke(stroke, cap = StrokeCap.Butt, join = StrokeJoin.Round))
}

private fun loaderContourPoints(center: Offset, radius: Float, morph: Float): List<Offset> {
    val position = morph % loaderContours.size
    val shape = position.toInt()
    val from = loaderContours[shape]
    val to = loaderContours[(shape + 1) % loaderContours.size]
    return from.mapIndexed { index, point -> center + lerp(point, to[index], position - shape) * radius }
}

private fun Path.setLoaderContour(center: Offset, radius: Float, morph: Float) {
    reset()
    val points = loaderContourPoints(center, radius, morph)
    moveTo(points.first().x, points.first().y)
    points.forEachIndexed { index, point ->
        val previous = points[(index + points.size - 1) % points.size]
        val next = points[(index + 1) % points.size]
        val afterNext = points[(index + 2) % points.size]
        val control1 = point + (next - previous) / 6f
        val control2 = next - (afterNext - point) / 6f
        cubicTo(control1.x, control1.y, control2.x, control2.y, next.x, next.y)
    }
    close()
}

@Composable
private fun CoffeePeekMorphingLoaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePeekMorphingLoader() }
}

@Preview @Composable private fun CoffeePeekMorphingLoaderLightPreview() = CoffeePeekMorphingLoaderPreview(false)
@Preview @Composable private fun CoffeePeekMorphingLoaderDarkPreview() = CoffeePeekMorphingLoaderPreview(true)
