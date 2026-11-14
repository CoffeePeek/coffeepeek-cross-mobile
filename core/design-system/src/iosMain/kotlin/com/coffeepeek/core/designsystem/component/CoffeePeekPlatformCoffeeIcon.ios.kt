@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.posix.memcpy
import org.jetbrains.skia.Image

@Composable
actual fun CoffeePeekPlatformCoffeeIcon(contentDescription: String?, modifier: Modifier, tint: Color) {
    val bitmap = remember { coffeeSymbolBitmap() }
    Icon(bitmap, contentDescription, modifier, tint)
}

private fun coffeeSymbolBitmap(): ImageBitmap {
    val configuration = UIImageSymbolConfiguration.configurationWithPointSize(24.0)
    val symbol = checkNotNull(UIImage.systemImageNamed("cup.and.saucer", withConfiguration = configuration))
    // Rasterize into Compose so the marker remains above the native map without an interop cut-out.
    val renderer = UIGraphicsImageRenderer(size = symbol.size)
    val bounds = symbol.size.useContents { CGRectMake(0.0, 0.0, width, height) }
    val png = renderer.PNGDataWithActions { symbol.drawInRect(bounds) }
    val bytes = ByteArray(png.length.toInt())
    bytes.usePinned { memcpy(it.addressOf(0), png.bytes, png.length) }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
