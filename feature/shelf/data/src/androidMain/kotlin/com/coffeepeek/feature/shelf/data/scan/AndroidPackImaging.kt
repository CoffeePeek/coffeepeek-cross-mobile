package com.coffeepeek.feature.shelf.data.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import com.coffeepeek.feature.shelf.data.scan.imaging.Quad
import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

/** The scan's image work on Android's Bitmap, Canvas and ImageDecoder. */
internal object AndroidPackImaging : PackImaging {
    override fun decode(photo: ByteArray, maxSide: Int): PackImage = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(photo))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val factor = maxSide.toFloat() / max(info.size.width, info.size.height)
            if (factor < 1f) decoder.setTargetSize((info.size.width * factor).roundToInt(), (info.size.height * factor).roundToInt())
        }.copy(Bitmap.Config.ARGB_8888, false).image()
    } catch (_: java.io.IOException) {
        throw BagScanFailure(BagScanIssue.UnreadablePhoto)
    }

    override fun scaledDown(image: PackImage, maxSide: Int): PackImage {
        val factor = maxSide.toFloat() / max(image.width, image.height)
        if (factor >= 1f) return image
        return Bitmap.createScaledBitmap(image.bitmap(), (image.width * factor).roundToInt(), (image.height * factor).roundToInt(), true).image()
    }

    // Perspective-stretches [quad] (photo pixels) onto an opaque upright rectangle at full detail, then scales it down.
    override fun rectified(image: PackImage, quad: Quad, width: Int, height: Int): PackImage {
        val (fullWidth, fullHeight) = quad.rectangle().let { (w, h) -> max(width, w.roundToInt()) to max(height, h.roundToInt()) }
        val source = FloatArray(8) { quad.c[it].toFloat() }
        val target = floatArrayOf(0f, 0f, fullWidth.toFloat(), 0f, fullWidth.toFloat(), fullHeight.toFloat(), 0f, fullHeight.toFloat())
        val matrix = Matrix().apply { setPolyToPoly(source, 0, target, 0, 4) }
        val large = Bitmap.createBitmap(fullWidth, fullHeight, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply { drawColor(Color.BLACK) }.drawBitmap(image.bitmap(), matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        return Bitmap.createScaledBitmap(large, width, height, true).image()
    }

    override fun jpeg(image: PackImage, quality: Int): ByteArray =
        ByteArrayOutputStream().also { image.bitmap().compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
}

private fun Bitmap.image() = PackImage(width, height, IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) })

private fun PackImage.bitmap(): Bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
