@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate

/** Keeps the actual design-system icon, including custom vectors without an SF Symbol equivalent. */
@Composable
internal fun coffeePeekNativeImage(vector: ImageVector): UIImage? {
    return coffeePeekNativeImage(rememberVectorPainter(vector))
}

@Composable
internal fun coffeePeekNativeImage(painter: Painter): UIImage? {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    var image by remember(painter, density, direction) { mutableStateOf<UIImage?>(null) }
    // Render after the vector painter's composition has been applied.
    LaunchedEffect(painter, density, direction) {
        image = coffeePeekRenderNativeImage(painter, density, direction)
    }
    return image
}

private fun coffeePeekRenderNativeImage(painter: Painter, density: Density, direction: LayoutDirection): UIImage {
    val side = (20 * density.density).toInt().coerceAtLeast(1)
    val bitmap = ImageBitmap(side, side)
    CanvasDrawScope().draw(density, direction, Canvas(bitmap), Size(side.toFloat(), side.toFloat())) {
        with(painter) { draw(size) }
    }
    val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    try {
        val data = checkNotNull(image.encodeToData())
        try { return coffeePeekTemplateImage(data.bytes, density.density.toDouble()) }
        finally { data.close() }
    } finally {
        image.close()
        bitmap.asSkiaBitmap().close()
    }
}

private fun coffeePeekTemplateImage(bytes: ByteArray, scale: Double): UIImage = bytes.usePinned {
    val data = NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
    checkNotNull(UIImage.imageWithData(data, scale)).imageWithRenderingMode(UIImageRenderingModeAlwaysTemplate)
}
