package com.coffeepeek.core.media

import androidx.compose.ui.graphics.ImageBitmap
import com.coffeepeek.core.media.image.IosImageBitmapDecoder
import io.kamel.core.config.ResourceConfigBuilder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.currentCoroutineContext

/** Decodes in-memory image bytes downsampled to [maxDimension]; the same bounded decoder the image loader uses. */
suspend fun decodeBoundedBitmap(bytes: ByteArray, maxDimension: Int): ImageBitmap? =
    IosImageBitmapDecoder(maxDimension).decode(ByteReadChannel(bytes), ResourceConfigBuilder(currentCoroutineContext()).build())
