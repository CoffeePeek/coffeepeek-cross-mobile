package com.coffeepeek.admin.feature.checkin.data

import android.graphics.Bitmap
import android.graphics.Color
import com.coffeepeek.admin.CoffeePeekApplication
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.admin.utils.BitmapUtil
import com.coffeepeek.admin.utils.BitmapUtil.changeSize
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Both models ship with the APK: direct installs and the first offline check-in work too. */
internal actual fun createDrinkPhotoExtractor(): DrinkPhotoExtractor = DrinkPhotoExtractor { photo ->
    withContext(Dispatchers.Default) {
        val decoded = BitmapUtil.decodeOrientedBitmap(photo.bytes, decodeMaxSide = 2048)
        val bitmap = decoded.changeSize(1024)
        if (bitmap !== decoded) decoded.recycle()
        try {
            val context = CoffeePeekApplication.context
            val detectorOptions = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("checkin/efficientdet_lite0.tflite").build())
                .setCategoryAllowlist(listOf("cup", "wine glass", "bottle"))
                .setScoreThreshold(0.3f)
                .setMaxResults(4)
                .build()
            val input = BitmapImageBuilder(bitmap).build()
            try {
                val cups = ObjectDetector.createFromOptions(context, detectorOptions).use { detector ->
                    detector.detect(input).detections().map { it.boundingBox() }
                }
                ensureActive()
                if (cups.isEmpty()) return@withContext emptyList()
                val options = InteractiveSegmenter.InteractiveSegmenterOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("checkin/magic_touch.tflite").build())
                    .setOutputConfidenceMasks(true)
                    .setOutputCategoryMask(false)
                    .build()
                InteractiveSegmenter.createFromOptions(context, options).use { segmenter ->
                    cups.mapIndexedNotNull { index, bounds ->
                        ensureActive()
                        // Target the vessel rather than merging all touching foreground objects.
                        // MediaPipe 0.10.35's Java adapter omits Point.normalized=true, so its
                        // native ROI graph expects pixels despite the NormalizedKeypoint type.
                        // Keep this conversion tied to the pinned version (see model README).
                        val point = NormalizedKeypoint.create(
                            bounds.centerX().coerceIn(0f, (bitmap.width - 1).toFloat()),
                            bounds.centerY().coerceIn(0f, (bitmap.height - 1).toFloat()),
                        )
                        val result = segmenter.segment(input, InteractiveSegmenter.RegionOfInterest.create(point))
                        val masks = result.confidenceMasks().orElse(emptyList())
                        try {
                            // MagicTouch v1 has one foreground confidence channel.
                            val mask = masks.singleOrNull() ?: error("Unexpected cup mask format")
                            check(mask.width == bitmap.width && mask.height == bitmap.height)
                            val buffer = ByteBufferExtractor.extract(mask).order(ByteOrder.nativeOrder()).asFloatBuffer()
                            val confidence = FloatArray(buffer.remaining()).also(buffer::get)
                            val cutout = maskDrinkSubject(bitmap, confidence) ?: return@mapIndexedNotNull null
                            try { encodeDrinkSubject(cutout, index) } finally { cutout.recycle() }
                        } finally {
                            masks.forEach { it.close() }
                        }
                    }
                }.also { ensureActive() }
            } finally {
                input.close()
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}

/** Apply a soft object mask, then crop to its visible extent with transparent padding. */
internal fun maskDrinkSubject(source: Bitmap, confidence: FloatArray): Bitmap? {
    require(confidence.size == source.width * source.height)
    val pixels = IntArray(confidence.size)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    var left = source.width
    var top = source.height
    var right = -1
    var bottom = -1
    for (index in pixels.indices) {
        // Remove uncertain background; retain an antialiased transition at the cup's edge.
        val value = confidence[index].takeIf(Float::isFinite) ?: 0f
        val alpha = (((value - 0.2f) / 0.6f).coerceIn(0f, 1f) * Color.alpha(pixels[index])).roundToInt()
        pixels[index] = (pixels[index] and 0x00ffffff) or (alpha shl 24)
        if (alpha > 0) {
            val x = index % source.width
            val y = index / source.width
            left = minOf(left, x)
            right = maxOf(right, x)
            top = minOf(top, y)
            bottom = maxOf(bottom, y)
        }
    }
    if (right < left || bottom < top) return null
    val width = right - left + 1
    val height = bottom - top + 1
    return Bitmap.createBitmap(width + 4, height + 4, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, top * source.width + left, source.width, 2, 2, width, height)
    }
}

/** Keep only usable transparent cutouts; an opaque crop would fail the upload contract. */
internal fun encodeDrinkSubject(cutout: Bitmap, index: Int): PendingPhotoUpload? {
    val sized = cutout.changeSize(768)
    return try {
        if (!sized.hasAlpha()) return null
        val pixels = IntArray(sized.width * sized.height)
        sized.getPixels(pixels, 0, sized.width, 0, 0, sized.width, sized.height)
        if (pixels.none { Color.alpha(it) < 255 } || pixels.none { Color.alpha(it) > 0 }) return null
        val bytes = ByteArrayOutputStream().use { output ->
            check(sized.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.toByteArray()
        }
        check(bytes.size in 1..4_194_304) { "Недопустимый размер изображения чашки" }
        PendingPhotoUpload("drink_$index.png", "image/png", bytes)
    } finally {
        if (sized !== cutout) sized.recycle()
    }
}
