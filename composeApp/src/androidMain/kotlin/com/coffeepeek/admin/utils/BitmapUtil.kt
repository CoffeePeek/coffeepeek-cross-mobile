package com.coffeepeek.admin.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import kotlin.io.encoding.Base64
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtil {

    /** Limits for on-screen image decoding (memory-friendly). */
    const val DISPLAY_MAX_SIDE = 1_280
    val DISPLAY_BYTE_RANGE: IntRange = 200_000..280_000

    /** Limits for photos uploaded to the backend (shop / review / avatar). */
    const val UPLOAD_MAX_SIDE = 2_560
    const val UPLOAD_MAX_BYTES = 4 * 1024 * 1024 // stay under the 5MB avatar cap
    const val UPLOAD_JPEG_QUALITY = 92
    const val UPLOAD_MIN_JPEG_QUALITY = 78

    fun load(
        contentResolver: ContentResolver,
        uri: Uri,
        maxHeightWidth: Int = DISPLAY_MAX_SIDE,
        maxByteSize: IntRange = DISPLAY_BYTE_RANGE,
    ): Bitmap {
        val stream = contentResolver.openInputStream(uri) ?: throw FileNotFoundException()
        val bytes = stream.use { it.readBytes() }
        return load(bytes, maxHeightWidth, maxByteSize)
    }

    fun load(
        byteArray: ByteArray,
        maxHeightWidth: Int = DISPLAY_MAX_SIDE,
        maxByteSize: IntRange = DISPLAY_BYTE_RANGE,
    ): Bitmap {
        val oriented = decodeOrientedBitmap(byteArray, decodeMaxSide = maxHeightWidth * 2)
        return oriented
            .changeSize(maxHeightWidth)
            .compress(maxByteSize)
    }

    /** Display downloads must retain PNG transparency, including on Android before API 30. */
    internal fun prepareForDisplay(byteArray: ByteArray): ByteArray {
        val decoded = decodeOrientedBitmap(byteArray, decodeMaxSide = DISPLAY_MAX_SIDE * 2)
        val sized = decoded.changeSize(DISPLAY_MAX_SIDE)
        return try {
            if (sized.hasAlpha()) sized.toByteArray(Bitmap.CompressFormat.PNG)
            else sized.compressToSize(DISPLAY_BYTE_RANGE)
        } finally {
            if (sized !== decoded) sized.recycle()
            decoded.recycle()
        }
    }

    /**
     * Prepares a gallery/camera photo for upload: keeps high resolution and quality,
     * only downscales/compresses when needed to stay under [UPLOAD_MAX_BYTES].
     */
    fun prepareForUpload(byteArray: ByteArray): ByteArray {
        var bitmap = decodeOrientedBitmap(byteArray, decodeMaxSide = UPLOAD_MAX_SIDE * 2)
            .changeSize(UPLOAD_MAX_SIDE)
        try {
            return encodeJpegForUpload(bitmap)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun prepareForUpload(bitmap: Bitmap): ByteArray {
        val sized = bitmap.changeSize(UPLOAD_MAX_SIDE)
        return try {
            encodeJpegForUpload(sized)
        } finally {
            if (sized !== bitmap && !sized.isRecycled) sized.recycle()
        }
    }

    private fun encodeJpegForUpload(source: Bitmap): ByteArray {
        var working: Bitmap = source
        var ownsWorking = false
        try {
            var side = max(working.width, working.height)
            while (true) {
                val atStartQuality = working.toJpegBytes(UPLOAD_JPEG_QUALITY)
                if (atStartQuality.size <= UPLOAD_MAX_BYTES) return atStartQuality

                val qualityFitted = compressJpegToMaxBytes(
                    bitmap = working,
                    maxBytes = UPLOAD_MAX_BYTES,
                    startQuality = UPLOAD_JPEG_QUALITY,
                    minQuality = UPLOAD_MIN_JPEG_QUALITY,
                )
                if (qualityFitted.size <= UPLOAD_MAX_BYTES) return qualityFitted

                if (side <= 960) return qualityFitted

                side = (side * 0.85f).roundToInt().coerceAtLeast(960)
                val smaller = working.changeSize(side)
                if (ownsWorking && working !== source && !working.isRecycled) {
                    working.recycle()
                }
                working = smaller
                ownsWorking = smaller !== source
            }
        } finally {
            if (ownsWorking && working !== source && !working.isRecycled) {
                working.recycle()
            }
        }
    }

    private fun compressJpegToMaxBytes(
        bitmap: Bitmap,
        maxBytes: Int,
        startQuality: Int,
        minQuality: Int,
    ): ByteArray {
        var lo = minQuality
        var hi = startQuality
        var best = bitmap.toJpegBytes(minQuality)
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val candidate = bitmap.toJpegBytes(mid)
            if (candidate.size <= maxBytes) {
                best = candidate
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return best
    }

    internal fun decodeOrientedBitmap(byteArray: ByteArray, decodeMaxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, bounds)

        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxSide = decodeMaxSide.coerceAtLeast(1),
        )
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
            ?: error("Failed to decode image")

        val exif = ExifInterface(byteArray.inputStream())
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return decoded

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded && !decoded.isRecycled) decoded.recycle()
        return rotated
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) / 2 >= maxSide) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample.coerceAtLeast(1)
    }

    fun Bitmap.changeSize(maxSize: Int): Bitmap {
        val longest = max(width, height)
        if (longest <= maxSize || maxSize <= 0) return this

        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width >= height) {
            maxSize to (maxSize / ratio).roundToInt().coerceAtLeast(1)
        } else {
            (maxSize * ratio).roundToInt().coerceAtLeast(1) to maxSize
        }
        return scale(newWidth, newHeight)
    }

    fun Bitmap.compressToSize(sizeRange: IntRange): ByteArray {
        var minq = 0
        var maxq = 100
        var stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, stream)
        var guard = 0
        while (
            (stream.size() > sizeRange.last || stream.size() < sizeRange.first) &&
            guard < 12
        ) {
            val q = (maxq + minq) / 2
            if (q < 2) break
            stream = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.JPEG, q, stream)
            when {
                stream.size() > sizeRange.last -> maxq = q
                stream.size() < sizeRange.first -> minq = q
                else -> break
            }
            guard++
        }
        return stream.toByteArray()
    }

    fun Bitmap.compress(sizeRange: IntRange): Bitmap {
        val arr = compressToSize(sizeRange)
        return BitmapFactory.decodeByteArray(arr, 0, arr.size) ?: this
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), stream)
        return stream.toByteArray()
    }

    fun Bitmap.toByteArray(
        format: Bitmap.CompressFormat? = null,
        quality: Int = 100,
    ): ByteArray {
        val type = when {
            format != null -> format
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSLESS
            else -> Bitmap.CompressFormat.JPEG
        }

        val stream = ByteArrayOutputStream()
        compress(type, quality, stream)
        return stream.toByteArray()
    }

    fun ByteArray.toBase64(): String {
        return Base64.encode(this, 0, this.size)
    }
}
