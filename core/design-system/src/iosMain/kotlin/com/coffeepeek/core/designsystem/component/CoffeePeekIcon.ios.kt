@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.posix.memcpy

@Composable
internal actual fun coffeePeekSystemSymbolPainter(icon: ImageVector): Painter? {
    val name = coffeePeekSymbolName(icon) ?: return null
    val density = LocalDensity.current.density
    return remember(name, icon.defaultWidth, icon.defaultHeight, density) {
        coffeePeekSymbolBitmaps.getOrPut(name) { coffeePeekSymbolBitmap(name) }?.let { bitmap ->
            CoffeePeekSymbolPainter(bitmap, Size(icon.defaultWidth.value * density, icon.defaultHeight.value * density))
        }
    }
}

internal fun coffeePeekSystemImage(icon: ImageVector): UIImage? =
    coffeePeekSymbolName(icon)?.let { UIImage.systemImageNamed(it) }

// Only the finite set of shared symbols is rasterized. Every tint and size reuses the same mask.
private val coffeePeekSymbolBitmaps = mutableMapOf<String, ImageBitmap?>()

private fun coffeePeekSymbolBitmap(name: String): ImageBitmap? {
    val configuration = UIImageSymbolConfiguration.configurationWithPointSize(32.0)
    val image = UIImage.systemImageNamed(name, withConfiguration = configuration) ?: return null
    val bounds = image.size.useContents { CGRectMake(0.0, 0.0, width, height) }
    val png = UIGraphicsImageRenderer(size = image.size).PNGDataWithActions { image.drawInRect(bounds) }
    val bytes = ByteArray(png.length.toInt())
    if (bytes.isEmpty()) return null
    bytes.usePinned { memcpy(it.addressOf(0), png.bytes, png.length) }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

private class CoffeePeekSymbolPainter(private val bitmap: ImageBitmap, override val intrinsicSize: Size) : Painter() {
    private val painter = BitmapPainter(bitmap)

    override fun DrawScope.onDraw() {
        val scale = minOf(size.width / bitmap.width, size.height / bitmap.height) * 0.84f
        val horizontalPadding = (size.width - bitmap.width * scale) / 2
        val verticalPadding = (size.height - bitmap.height * scale) / 2
        inset(horizontalPadding, verticalPadding) { with(painter) { draw(size) } }
    }
}
