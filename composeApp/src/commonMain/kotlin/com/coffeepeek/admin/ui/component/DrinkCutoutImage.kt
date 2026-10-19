package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.cpImageData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Keeps the transparent photo intact; the white edge follows its alpha silhouette. */
@Composable
internal fun DrinkCutoutImage(
    data: Any,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    outlineWidth: Dp = 2.dp,
) {
    val source = com.coffeepeek.admin.utils.rememberCpImagePainter(cpImageData(data))
    val width = with(LocalDensity.current) { outlineWidth.toPx() }
    val painter = remember(source, width) { DrinkCutoutPainter(source, width) }
    Image(painter, contentDescription, modifier, contentScale = ContentScale.Fit)
}

internal class DrinkCutoutPainter(
    private val source: Painter,
    private val outlineWidthPx: Float,
) : Painter() {
    override val intrinsicSize: Size get() = source.intrinsicSize
    private val silhouette = ColorFilter.tint(Color.White, BlendMode.SrcIn)

    override fun DrawScope.onDraw() {
        val width = outlineWidthPx.coerceIn(0f, size.minDimension / 4f)
        inset(width) {
            val intrinsic = source.intrinsicSize
            val photoSize = if (intrinsic != Size.Unspecified && intrinsic.width > 0 && intrinsic.height > 0) {
                val scale = minOf(size.width / intrinsic.width, size.height / intrinsic.height)
                Size(intrinsic.width * scale, intrinsic.height * scale)
            } else size
            // Reserve space inside the painter so the edge is visible even if the PNG
            // is cropped tightly against the object. Tint preserves the PNG's alpha.
            translate((size.width - photoSize.width) / 2, (size.height - photoSize.height) / 2) {
                with(source) {
                    draw(photoSize, colorFilter = silhouette)
                    outlineDirections.forEach { direction ->
                        translate(direction.x * width, direction.y * width) {
                            draw(photoSize, colorFilter = silhouette)
                        }
                    }
                    draw(photoSize)
                }
            }
        }
    }
}

private val outlineDirections = List(16) { index ->
    val angle = index * 2 * PI / 16
    Offset(cos(angle).toFloat(), sin(angle).toFloat())
}
