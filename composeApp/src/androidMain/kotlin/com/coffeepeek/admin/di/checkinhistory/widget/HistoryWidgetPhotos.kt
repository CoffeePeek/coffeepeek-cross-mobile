package com.coffeepeek.admin.di.checkinhistory.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.coffeepeek.admin.utils.CustomUrl
import com.coffeepeek.admin.utils.CustomUrlFetcher
import com.coffeepeek.admin.utils.cpImageData
import com.coffeepeek.admin.utils.image.ProtectedPhotoSession
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryActivityMonth
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfigBuilder
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.readByteArray

/** Reuses the authenticated photo transport. Only small bitmaps cross into the launcher. */
internal class HistoryWidgetPhotos(private val fetcher: CustomUrlFetcher, private val sessions: SessionReader) {
    suspend fun load(month: HistoryActivityMonth): Map<String, Bitmap> = coroutineScope {
        val limit = Semaphore(4)
        val permission = ProtectedPhotoSession.scope(sessions.peekSession())
        month.days.mapNotNull { (date, day) -> day.photo?.let { photo ->
            async { limit.withPermit {
                val bitmap = withTimeoutOrNull(6_000) { read(photo.url, permission, photo.isCutout) }
                bitmap?.let { date to it }
            } }
        } }.awaitAll().filterNotNull().toMap()
    }

    private suspend fun read(url: String, permission: Any, cutout: Boolean): Bitmap? = try {
        val data = (cpImageData(url) as CustomUrl).copy(permissionScope = permission)
        val config = ResourceConfigBuilder(currentCoroutineContext()).build()
        val result = fetcher.fetch(data, config).first { it !is Resource.Loading }
        val channel = (result as? Resource.Success)?.value
        val bytes = channel?.let { try { it.readRemaining(8L * 1024 * 1024 + 1).readByteArray() }
            finally { it.cancel(null) } }
        if (bytes == null || bytes.size > 8 * 1024 * 1024 || !ProtectedPhotoSession.current(permission)) null
        else thumbnail(bytes, cutout)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    private fun thumbnail(bytes: ByteArray, cutout: Boolean): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
        while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 256) options.inSampleSize *= 2
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val target = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        val scale = if (cutout) 72f / maxOf(source.width, source.height) else 72f / minOf(source.width, source.height)
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        val left = (72 - width) / 2
        val top = (72 - height) / 2
        Canvas(target).drawBitmap(source, null, Rect(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG))
        source.recycle()
        return target
    }
}
