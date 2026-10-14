package com.coffeepeek.admin.di.shop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_DRINK_PHOTO_BYTES
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_DRINK_PHOTO_EDGE
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

/** Android bridge only. Gallery/camera already normalize orientation before delivering these bytes. */
internal class AndroidShopDrinkPhotoProcessor : ShopDrinkPhotoProcessor {
    override suspend fun process(source: ShopCheckInPhoto): Result<List<ShopCheckInPhoto>> = withContext(Dispatchers.Default) {
        try {
            val image = decodeBounded(source.bytes)
            val client = try {
                val subjects = SubjectSegmenterOptions.SubjectResultOptions.Builder()
                    .enableSubjectBitmap()
                    .build()
                SubjectSegmentation.getClient(
                    SubjectSegmenterOptions.Builder().enableMultipleSubjects(subjects).build(),
                )
            } catch (error: Exception) {
                image.recycle()
                throw error
            }
            val task = try {
                client.process(InputImage.fromBitmap(image, 0))
            } catch (error: Exception) {
                client.close()
                image.recycle()
                throw error
            }
            try {
                val result = task.awaitResult()
                val photos = result.subjects.mapIndexedNotNull { index, subject ->
                    ensureActive()
                    subject.bitmap?.toShopDrinkPhoto(index)
                }
                require(photos.isNotEmpty()) { "No foreground objects found" }
                Result.success(photos)
            } finally {
                // Task cancellation does not stop native inference. Retain its bitmaps until inference ends.
                val release = {
                    if (task.isSuccessful) task.result.subjects.mapNotNull { it.bitmap }.distinct().forEach {
                        if (!it.isRecycled) it.recycle()
                    }
                    if (!image.isRecycled) image.recycle()
                    client.close()
                }
                if (task.isComplete) release() else task.addOnCompleteListener { release() }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

private fun decodeBounded(bytes: ByteArray): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid source image" }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
}

internal fun Bitmap.toShopDrinkPhoto(index: Int): ShopCheckInPhoto? {
    val limit = MAX_SHOP_DRINK_PHOTO_EDGE - 2
    val scale = minOf(1f, limit.toFloat() / max(width, height))
    val scaled = Bitmap.createScaledBitmap(this, (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1), true)
    // A transparent border also preserves transparency when a subject fills its bounding rectangle.
    val png = Bitmap.createBitmap(scaled.width + 2, scaled.height + 2, Bitmap.Config.ARGB_8888)
    try {
        png.eraseColor(Color.TRANSPARENT)
        Canvas(png).drawBitmap(scaled, 1f, 1f, null)
        val pixels = IntArray(png.width * png.height)
        png.getPixels(pixels, 0, png.width, 0, 0, png.width, png.height)
        if (pixels.none { Color.alpha(it) > 0 }) return null
        val bytes = ByteArrayOutputStream().use { output ->
            check(png.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode foreground PNG" }
            output.toByteArray()
        }
        require(bytes.size <= MAX_SHOP_DRINK_PHOTO_BYTES) { "Foreground PNG is too large" }
        return ShopCheckInPhoto(bytes, "drink_$index.png", "image/png")
    } finally {
        png.recycle()
        if (scaled !== this) scaled.recycle()
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        when {
            task.isCanceled -> continuation.cancel(CancellationException("Subject extraction cancelled"))
            task.isSuccessful -> continuation.resume(task.result)
            else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Subject extraction failed"))
        }
    }
}
