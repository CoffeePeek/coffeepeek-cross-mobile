package com.coffeepeek.admin.di.collections

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.coffeepeek.admin.utils.BitmapUtil
import kotlinx.coroutines.*

internal actual suspend fun decodeCollectionPosterPhoto(bytes: ByteArray): ImageBitmap? = withContext(Dispatchers.Default) {
    try { BitmapUtil.decodeOrientedBitmap(bytes, 256).asImageBitmap() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
}
