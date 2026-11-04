package com.coffeepeek.admin.di.collections

import androidx.compose.ui.graphics.asSkiaBitmap
import com.coffeepeek.admin.utils.image.IosImageBitmapDecoder
import io.kamel.core.config.ResourceConfigBuilder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.currentCoroutineContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

private val collectionCoverDecoder = IosImageBitmapDecoder(maxDimension = 1280)

internal actual suspend fun prepareCollectionCover(bytes: ByteArray): ByteArray {
    val bitmap = collectionCoverDecoder.decode(ByteReadChannel(bytes), ResourceConfigBuilder(currentCoroutineContext()).build())
    val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    try {
        for (quality in 90 downTo 40 step 10) {
            val data = image.encodeToData(EncodedImageFormat.JPEG, quality) ?: continue
            val encoded = try { data.bytes } finally { data.close() }
            if (encoded.size <= 1024 * 1024) return encoded
        }
        error("Collection cover exceeds its byte budget")
    } finally { image.close() }
}
