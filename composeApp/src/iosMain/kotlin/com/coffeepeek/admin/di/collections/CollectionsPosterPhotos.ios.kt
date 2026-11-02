package com.coffeepeek.admin.di.collections

import androidx.compose.ui.graphics.ImageBitmap
import com.coffeepeek.admin.utils.image.IosImageBitmapDecoder
import io.kamel.core.config.ResourceConfigBuilder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext

private val collectionPhotoDecoder = IosImageBitmapDecoder(maxDimension = 256)

internal actual suspend fun decodeCollectionPosterPhoto(bytes: ByteArray): ImageBitmap? = try {
    collectionPhotoDecoder.decode(ByteReadChannel(bytes), ResourceConfigBuilder(currentCoroutineContext()).build())
} catch (cancelled: CancellationException) { throw cancelled }
catch (_: Exception) { null }
