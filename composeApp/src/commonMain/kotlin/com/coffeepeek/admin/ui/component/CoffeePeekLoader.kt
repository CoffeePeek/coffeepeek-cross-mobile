package com.coffeepeek.admin.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ROTATION_DURATION_MS = 1600
private const val FILL_DURATION_MS = 2800
private const val MORPH_DURATION_MS = 5000
private const val MAX_FILL_FRACTION = 0.72f

// Five reference contours, sampled clockwise from the top at equal angles.
private val loaderContourRadii = arrayOf(
    floatArrayOf(
        0.6816f, 0.6813f, 0.6818f, 0.6750f, 0.6671f, 0.6592f, 0.6575f, 0.6435f,
        0.6462f, 0.6465f, 0.6582f, 0.6696f, 0.6809f, 0.6956f, 0.7027f, 0.7200f,
        0.7372f, 0.7491f, 0.7693f, 0.7921f, 0.8173f, 0.8455f, 0.8708f, 0.8737f,
        0.8519f, 0.8268f, 0.7958f, 0.7670f, 0.7468f, 0.7380f, 0.7276f, 0.7164f,
        0.7037f, 0.7017f, 0.7034f, 0.7066f, 0.7112f, 0.7217f, 0.7374f, 0.7619f,
        0.7794f, 0.7915f, 0.8008f, 0.8020f, 0.7995f, 0.7919f, 0.7694f, 0.7579f,
        0.7560f, 0.7546f, 0.7494f, 0.7572f, 0.7678f, 0.7956f, 0.8129f, 0.7978f,
        0.7852f, 0.7750f, 0.7652f, 0.7577f, 0.7472f, 0.7350f, 0.7122f, 0.6852f,
    ),
    floatArrayOf(
        0.7061f, 0.7208f, 0.7323f, 0.7391f, 0.7309f, 0.7141f, 0.7014f, 0.6880f,
        0.6837f, 0.6993f, 0.7189f, 0.7382f, 0.7571f, 0.7726f, 0.7896f, 0.7968f,
        0.7963f, 0.7968f, 0.8076f, 0.8201f, 0.8462f, 0.8646f, 0.8762f, 0.8681f,
        0.8570f, 0.8356f, 0.8068f, 0.7774f, 0.7479f, 0.7384f, 0.7227f, 0.7210f,
        0.7191f, 0.7174f, 0.7163f, 0.7223f, 0.7313f, 0.7401f, 0.7469f, 0.7577f,
        0.7454f, 0.7354f, 0.7311f, 0.7237f, 0.7238f, 0.7299f, 0.7334f, 0.7455f,
        0.7676f, 0.7887f, 0.8085f, 0.8309f, 0.8531f, 0.8751f, 0.9073f, 0.9270f,
        0.9201f, 0.8700f, 0.8008f, 0.7401f, 0.6816f, 0.6457f, 0.6532f, 0.6762f,
    ),
    floatArrayOf(
        0.7759f, 0.7574f, 0.7443f, 0.7264f, 0.7240f, 0.7705f, 0.8023f, 0.8105f,
        0.8039f, 0.7763f, 0.7284f, 0.7132f, 0.7126f, 0.7216f, 0.7341f, 0.7532f,
        0.7758f, 0.8092f, 0.8321f, 0.8575f, 0.8844f, 0.9184f, 0.9536f, 0.9870f,
        0.9693f, 0.8923f, 0.8304f, 0.7816f, 0.7542f, 0.7530f, 0.7476f, 0.7678f,
        0.7779f, 0.7812f, 0.7781f, 0.7785f, 0.7720f, 0.7637f, 0.7606f, 0.7606f,
        0.7600f, 0.7778f, 0.7983f, 0.8130f, 0.8298f, 0.8317f, 0.8319f, 0.8294f,
        0.8203f, 0.8294f, 0.8323f, 0.8467f, 0.8732f, 0.8717f, 0.8632f, 0.8519f,
        0.8490f, 0.8345f, 0.8255f, 0.8163f, 0.8117f, 0.8070f, 0.7944f, 0.7812f,
    ),
    floatArrayOf(
        0.6254f, 0.6231f, 0.6077f, 0.5942f, 0.5831f, 0.5800f, 0.5711f, 0.5733f,
        0.5709f, 0.5882f, 0.6010f, 0.6107f, 0.6151f, 0.6220f, 0.6369f, 0.6572f,
        0.6893f, 0.7100f, 0.7348f, 0.7561f, 0.7746f, 0.7908f, 0.7800f, 0.7622f,
        0.7518f, 0.7329f, 0.7255f, 0.7146f, 0.6961f, 0.6707f, 0.6377f, 0.6126f,
        0.6054f, 0.6044f, 0.6000f, 0.6108f, 0.6209f, 0.6387f, 0.6671f, 0.7110f,
        0.7504f, 0.7770f, 0.7789f, 0.7834f, 0.7757f, 0.7503f, 0.7173f, 0.6978f,
        0.6965f, 0.6865f, 0.6935f, 0.7097f, 0.7247f, 0.7399f, 0.7563f, 0.7656f,
        0.7531f, 0.7358f, 0.6917f, 0.6575f, 0.6398f, 0.6380f, 0.6399f, 0.6425f,
    ),
    floatArrayOf(
        0.5699f, 0.5708f, 0.5771f, 0.5892f, 0.5980f, 0.5977f, 0.5990f, 0.6128f,
        0.6183f, 0.6357f, 0.6489f, 0.6671f, 0.6696f, 0.6713f, 0.6844f, 0.6942f,
        0.7088f, 0.7290f, 0.7556f, 0.7792f, 0.7897f, 0.8045f, 0.7988f, 0.7707f,
        0.7541f, 0.7214f, 0.6810f, 0.6545f, 0.6294f, 0.5991f, 0.5806f, 0.5622f,
        0.5541f, 0.5516f, 0.5597f, 0.5676f, 0.5800f, 0.6041f, 0.6217f, 0.6314f,
        0.6415f, 0.6589f, 0.6806f, 0.7011f, 0.7163f, 0.7231f, 0.7346f, 0.7440f,
        0.7451f, 0.7481f, 0.7527f, 0.7580f, 0.7712f, 0.7724f, 0.7607f, 0.7517f,
        0.7283f, 0.6975f, 0.6490f, 0.6223f, 0.6026f, 0.5903f, 0.5807f, 0.5708f,
    ),
)

private val loaderContours = loaderContourRadii.map { radii ->
    List(radii.size) { index ->
        val angle = (index * 2 * PI / radii.size - PI / 2).toFloat()
        Offset(cos(angle), sin(angle)) * radii[index]
    }
}

@Composable
fun CoffeePeekLoader(
    modifier: Modifier = Modifier,
    size: Dp = CpDimens.loaderDefault,
    color: Color = CpColor.Primary,
    strokeWidth: Dp = (size / 16f).coerceAtLeast(2.dp),
    contentDescription: String = "Загрузка",
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
        targetValue = loaderContours.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = MORPH_DURATION_MS
                for (shape in 0..loaderContours.size) {
                    shape.toFloat() at (shape * MORPH_DURATION_MS / loaderContours.size) using FastOutSlowInEasing
                }
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "coffeepeek-loader-morph",
    )
    val trackPath = remember { Path() }
    val fillPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                this.contentDescription = contentDescription
            },
    ) {
        val strokePx = strokeWidth.toPx()
        trackPath.setLoaderContour(
            center = center,
            radius = ((this.size.minDimension - strokePx) / 2f).coerceAtLeast(0f),
            morph = morph,
        )
        pathMeasure.setPath(trackPath, forceClosed = true)
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
            drawPath(
                path = trackPath,
                color = color.copy(alpha = 0.18f),
                style = trackStroke,
            )

            val fillEnd = (fillProgress * MAX_FILL_FRACTION).coerceIn(0f, MAX_FILL_FRACTION)
            if (fillEnd > 0.01f) {
                fillPath.reset()
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = pathMeasure.length * fillEnd,
                    destination = fillPath,
                )
                drawPath(
                    path = fillPath,
                    color = color,
                    style = fillStroke,
                )
            }
        }
    }
}

private fun Path.setLoaderContour(center: Offset, radius: Float, morph: Float) {
    reset()
    val position = morph % loaderContours.size
    val shape = position.toInt()
    val from = loaderContours[shape]
    val to = loaderContours[(shape + 1) % loaderContours.size]
    val fraction = position - shape
    val points = from.mapIndexed { index, point ->
        center + lerp(point, to[index], fraction) * radius
    }
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
