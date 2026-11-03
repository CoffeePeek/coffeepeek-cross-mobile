package com.coffeepeek.admin.di.collections

import androidx.compose.ui.graphics.ImageBitmap
import com.coffeepeek.core.media.decodeBoundedBitmap
import kotlinx.coroutines.CancellationException

internal actual suspend fun decodeCollectionPosterPhoto(bytes: ByteArray): ImageBitmap? = try {
    decodeBoundedBitmap(bytes, maxDimension = 512)
} catch (cancelled: CancellationException) { throw cancelled }
catch (_: Exception) { null }
