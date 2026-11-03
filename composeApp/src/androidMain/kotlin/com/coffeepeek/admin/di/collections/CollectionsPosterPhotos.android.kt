package com.coffeepeek.admin.di.collections

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.coffeepeek.core.media.AndroidImageCodec
import kotlinx.coroutines.*

internal actual suspend fun decodeCollectionPosterPhoto(bytes: ByteArray): ImageBitmap? = withContext(Dispatchers.Default) {
    try { AndroidImageCodec.decodeOrientedBitmap(bytes, 512).asImageBitmap() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
}
