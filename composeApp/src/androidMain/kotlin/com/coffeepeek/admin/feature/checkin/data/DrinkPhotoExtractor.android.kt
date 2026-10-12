package com.coffeepeek.admin.feature.checkin.data

import android.graphics.Bitmap
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.admin.utils.BitmapUtil
import com.coffeepeek.admin.utils.BitmapUtil.changeSize
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun createDrinkPhotoExtractor(): DrinkPhotoExtractor = DrinkPhotoExtractor { photo ->
    withContext(Dispatchers.Default) {
        val bitmap = BitmapUtil.load(photo.bytes, maxHeightWidth = 1024)
        val segmenter = try {
            SubjectSegmentation.getClient(
                SubjectSegmenterOptions.Builder().enableMultipleSubjects(
                    SubjectSegmenterOptions.SubjectResultOptions.Builder().enableSubjectBitmap().build(),
                ).build(),
            )
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        }
        suspendCancellableCoroutine { continuation ->
            try {
                segmenter.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnCompleteListener(Dispatchers.Default.asExecutor()) { task ->
                        val subjects = if (task.isSuccessful) task.result.subjects else emptyList()
                        try {
                            if (continuation.isActive) {
                                if (!task.isSuccessful) error("Не удалось выделить чашку. При первом запуске дождитесь загрузки модели и повторите попытку.")
                                val photos = subjects.mapIndexedNotNull { index, subject ->
                                    val cutout = subject.bitmap ?: return@mapIndexedNotNull null
                                    val sized = cutout.changeSize(768)
                                    try {
                                        val bytes = ByteArrayOutputStream().use { output ->
                                            check(sized.compress(Bitmap.CompressFormat.PNG, 100, output))
                                            output.toByteArray()
                                        }
                                        PendingPhotoUpload("drink_$index.png", "image/png", bytes)
                                    } finally {
                                        if (sized !== cutout) sized.recycle()
                                    }
                                }
                                continuation.resume(photos)
                            }
                        } catch (error: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(error)
                        } finally {
                            // ML Kit may still read the input after coroutine cancellation.
                            subjects.forEach { it.bitmap?.recycle() }
                            bitmap.recycle()
                            segmenter.close()
                        }
                    }
            } catch (error: Exception) {
                bitmap.recycle()
                segmenter.close()
                continuation.resumeWithException(error)
            }
        }
    }
}
