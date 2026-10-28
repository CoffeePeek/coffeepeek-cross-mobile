package com.coffeepeek.feature.shelf.data.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.os.SystemClock
import com.coffeepeek.feature.shelf.data.scan.imaging.*
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.usecase.PackNotFound
import com.coffeepeek.feature.shelf.domain.usecase.moderate
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Sandbox probe's pipeline: photo -> U2-Net-p subject -> refined front quad -> upright full rectangle with MI-GAN repair ->
 * gentle shading/glare softening -> JPEG card, plus pre-moderation. Each model is opened only for its stage, which keeps the peak low.
 */
internal class AndroidPackPipeline(private val models: () -> ScanModelFiles) {
    fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): BagScanResult {
        val clock = Clock()
        val full = decode(photo)
        val working = full.scaledToMaxSide(WORKING_SIDE)
        val rgb = RgbPlanes.of(working.pixels(), working.width, working.height)
        val probability = OnnxPackSegmenter(models().segmenter).use { it.probability(rgb) }
        val subject = largestRegion(BooleanArray(probability.data.size) { probability.data[it] > 0.5f }, working.width)
        val segmentation = clock.lap()
        onStage(BagScanStage.Segmentation)
        val front = fitFront(subject, working.width, working.height, rgb.gray())
        val fit = clock.lap()
        onStage(BagScanStage.Front)
        if (front == null) return BagScanResult(null, 0, 0, null, null, PackNotFound, BagScanTimings(segmentation, fit, 0, 0, clock.total()))
        onFront(front.quad.inset(INSET).frame(working))
        return finish(full, working, subject, front, clock, segmentation, fit, onStage)
    }

    private fun finish(
        full: Bitmap, working: Bitmap, subject: BooleanArray, front: FrontFit, clock: Clock, segmentation: Long, fit: Long,
        onStage: (BagScanStage) -> Unit,
    ): BagScanResult {
        val quad = front.quad.inset(INSET)
        val (width, height) = quad.outputSize(OUTPUT_SIDE)
        val rectified = full.rectified(quad.scaled(full.width.toDouble() / working.width), width, height)
        val repaint = repaintMask(missingAfterRectify(subject, working.width, working.height, quad, width, height), rectified.pixels(), width, height)
        val repaired = repair(rectified, repaint)
        val repairMillis = clock.lap()
        onStage(BagScanStage.Repair)
        val softened = soften(repaired.pixels(), width, height)
        val card = Bitmap.createBitmap(softened.pixels, width, height, Bitmap.Config.ARGB_8888).jpeg()
        val softenMillis = clock.lap()
        onStage(BagScanStage.Soften)
        val quality = frontQuality(front, subject, working.width, working.height)
            .copy(repaintShare = repaint.count { it }.toDouble() / repaint.size, strongGlare = softened.strongGlareShare.toDouble())
        return BagScanResult(ShelfBagCard(card, "image/jpeg", "bag.jpg"), width, height, quad.frame(working), quality, moderate(quality),
            BagScanTimings(segmentation, fit, repairMillis, softenMillis, clock.total()))
    }

    /** Without the inpainting model only a scan with nothing to repaint can go on. */
    private fun repair(rectified: Bitmap, repaint: BooleanArray): Bitmap {
        if (repaint.none { it }) return rectified
        val inpainter = models().inpainter ?: throw BagScanFailure(BagScanIssue.ModelMissing)
        return OnnxPackInpainter(inpainter).use { it.inpaint(rectified, repaint) }
    }

    private fun decode(photo: ByteArray): Bitmap = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(photo))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val factor = PHOTO_SIDE.toFloat() / max(info.size.width, info.size.height)
            if (factor < 1f) decoder.setTargetSize((info.size.width * factor).roundToInt(), (info.size.height * factor).roundToInt())
        }.copy(Bitmap.Config.ARGB_8888, false)
    } catch (_: java.io.IOException) {
        throw BagScanFailure(BagScanIssue.UnreadablePhoto)
    }

    private companion object {
        const val PHOTO_SIDE = 2400
        const val WORKING_SIDE = 1280
        const val OUTPUT_SIDE = 720
        const val INSET = 0.015
        const val JPEG_QUALITY = 85
    }

    private fun Bitmap.jpeg(): ByteArray = ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
}

/** Corners as shares of the working image, which has the photo's proportions. */
private fun Quad.frame(working: Bitmap) =
    BagScanFrame((0 until 4).map { (x(it) / working.width).toFloat() to (y(it) / working.height).toFloat() })

/** Model files for one scan; [inpainter] is null while MI-GAN is not downloaded yet. */
internal class ScanModelFiles(val segmenter: java.io.File, val inpainter: java.io.File?)

private class Clock {
    private val started = SystemClock.elapsedRealtime()
    private var last = started

    fun lap(): Long = SystemClock.elapsedRealtime().let { now -> (now - last).also { last = now } }

    fun total(): Long = SystemClock.elapsedRealtime() - started
}

private fun Bitmap.pixels(): IntArray = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

private fun Bitmap.scaledToMaxSide(maxSide: Int): Bitmap {
    val factor = maxSide.toFloat() / max(width, height)
    if (factor >= 1f) return this
    return Bitmap.createScaledBitmap(this, (width * factor).roundToInt(), (height * factor).roundToInt(), true)
}

// Perspective-stretches [quad] (photo pixels) onto an opaque upright rectangle at full detail, then scales it
// down; pixels outside the photo stay black and are part of the repaint mask.
private fun Bitmap.rectified(quad: Quad, width: Int, height: Int): Bitmap {
    val (fullWidth, fullHeight) = quad.rectangle().let { (w, h) -> max(width, w.roundToInt()) to max(height, h.roundToInt()) }
    val source = FloatArray(8) { quad.c[it].toFloat() }
    val target = floatArrayOf(0f, 0f, fullWidth.toFloat(), 0f, fullWidth.toFloat(), fullHeight.toFloat(), 0f, fullHeight.toFloat())
    val matrix = Matrix().apply { setPolyToPoly(source, 0, target, 0, 4) }
    val large = Bitmap.createBitmap(fullWidth, fullHeight, Bitmap.Config.ARGB_8888).also {
        Canvas(it).apply { drawColor(Color.BLACK) }.drawBitmap(this, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
    return Bitmap.createScaledBitmap(large, width, height, true)
}
