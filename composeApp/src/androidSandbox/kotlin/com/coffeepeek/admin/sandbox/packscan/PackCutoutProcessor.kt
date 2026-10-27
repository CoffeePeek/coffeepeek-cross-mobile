package com.coffeepeek.admin.sandbox.packscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.SystemClock
import com.coffeepeek.admin.sandbox.packscan.geometry.PackPoint
import com.coffeepeek.admin.sandbox.packscan.imaging.FrontFit
import com.coffeepeek.admin.sandbox.packscan.imaging.PackModeration
import com.coffeepeek.admin.sandbox.packscan.imaging.PackModerationReason
import com.coffeepeek.admin.sandbox.packscan.imaging.PackModerationStatus
import com.coffeepeek.admin.sandbox.packscan.imaging.PackQuality
import com.coffeepeek.admin.sandbox.packscan.imaging.Quad
import com.coffeepeek.admin.sandbox.packscan.imaging.RgbPlanes
import com.coffeepeek.admin.sandbox.packscan.imaging.fitFront
import com.coffeepeek.admin.sandbox.packscan.imaging.frontQuality
import com.coffeepeek.admin.sandbox.packscan.imaging.largestRegion
import com.coffeepeek.admin.sandbox.packscan.imaging.missingAfterRectify
import com.coffeepeek.admin.sandbox.packscan.imaging.moderate
import com.coffeepeek.admin.sandbox.packscan.imaging.outputSize
import com.coffeepeek.admin.sandbox.packscan.imaging.repaintMask
import com.coffeepeek.admin.sandbox.packscan.imaging.soften
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Every stage of one probe run, for the screen and the desktop comparison export. */
internal class PackCutoutResult(
    /** Working-size photo with the background dimmed and the chosen front quad drawn. */
    val overlay: Bitmap,
    /** Rectified front before repair; the repainted area is tinted. Null when no front was found. */
    val rectified: Bitmap?,
    /** Full rectangle after MI-GAN; null without the model or a front. */
    val inpainted: Bitmap?,
    val softened: Bitmap?,
    val card: Bitmap?,
    val quality: PackQuality?,
    val moderation: PackModeration,
    val start: String?,
    val timings: PackTimings,
)

internal class PackTimings(val segmentation: Long, val fit: Long, val repair: Long, val soften: Long, val total: Long)

/**
 * Probe-only, Google-free pipeline: photo -> U2-Net-p subject -> refined front quad -> upright full
 * rectangle with MI-GAN repair -> gentle shading/glare softening -> studio card, plus pre-moderation.
 * Both models are pushed with adb into the app's external files dir: Sandbox has no network.
 */
internal class PackCutoutProcessor(context: Context) : AutoCloseable {
    private val files = context.getExternalFilesDir(null)
    private val resolver = context.contentResolver
    val segmenterFile = File(files, SEGMENTER_FILE)
    val inpainterFile = File(files, INPAINTER_FILE)
    private val lazySegmenter = lazy { PackSegmenter(segmenterFile) }
    private val lazyInpainter = lazy { inpainterFile.takeIf { it.isFile }?.let(::PackInpainter) }

    suspend fun process(photo: Uri): PackCutoutResult = withContext(Dispatchers.Default) {
        val clock = Clock()
        val full = decode(photo)
        val working = full.scaledToMaxSide(WORKING_SIDE)
        val rgb = RgbPlanes.of(working.pixels(), working.width, working.height)
        val probability = lazySegmenter.value.probability(rgb)
        val subject = largestRegion(BooleanArray(probability.data.size) { probability.data[it] > 0.5f }, working.width)
        val segmentation = clock.lap()
        val front = fitFront(subject, working.width, working.height, rgb.gray())
        val fit = clock.lap()
        val overlay = working.overlay(subject, front?.quad)
        if (front == null) return@withContext notFound(overlay, PackTimings(segmentation, fit, 0, 0, clock.total()))
        finish(full, working, subject, front, overlay, clock, segmentation, fit)
    }

    private fun finish(
        full: Bitmap, working: Bitmap, subject: BooleanArray, front: FrontFit, overlay: Bitmap,
        clock: Clock, segmentation: Long, fit: Long,
    ): PackCutoutResult {
        val quad = front.quad.inset(INSET)
        val (width, height) = quad.outputSize(OUTPUT_SIDE)
        val rectified = full.rectified(quad.scaled(full.width.toDouble() / working.width), width, height)
        val missing = missingAfterRectify(subject, working.width, working.height, quad, width, height)
        val repaint = repaintMask(missing, rectified.pixels(), width, height)
        val inpainted = lazyInpainter.value?.inpaint(rectified, repaint)
        val repair = clock.lap()
        val softened = soften((inpainted ?: rectified).pixels(), width, height)
        val softBitmap = Bitmap.createBitmap(softened.pixels, width, height, Bitmap.Config.ARGB_8888)
        val soften = clock.lap()
        val quality = frontQuality(front, subject, working.width, working.height)
            .copy(repaintShare = repaint.count { it }.toDouble() / repaint.size, strongGlare = softened.strongGlareShare.toDouble())
        return PackCutoutResult(
            overlay, rectified.tinted(repaint), inpainted, softBitmap, studioCard(softBitmap), quality, moderate(quality),
            front.start, PackTimings(segmentation, fit, repair, soften, clock.total()),
        )
    }

    private fun notFound(overlay: Bitmap, timings: PackTimings) = PackCutoutResult(
        overlay, null, null, null, null, null,
        PackModeration(PackModerationStatus.Retake, listOf(PackModerationReason.EdgesNotFound)), null, timings,
    )

    /** Brush repair from the probe screen: repaints the stroked pixels of [image]. */
    suspend fun repaint(image: Bitmap, strokes: List<List<PackPoint>>): Pair<Bitmap, Long> = withContext(Dispatchers.Default) {
        val model = checkNotNull(lazyInpainter.value) { "MI-GAN model is missing: ${inpainterFile.path}" }
        val started = SystemClock.elapsedRealtime()
        val mask = strokeMask(image.width, image.height, strokes, image.brushRadius())
        model.inpaint(image, mask) to SystemClock.elapsedRealtime() - started
    }

    override fun close() {
        if (lazySegmenter.isInitialized()) lazySegmenter.value.close()
        if (lazyInpainter.isInitialized()) lazyInpainter.value?.close()
    }

    private fun decode(photo: Uri): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, photo)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val factor = PHOTO_SIDE.toFloat() / max(info.size.width, info.size.height)
            if (factor < 1f) decoder.setTargetSize((info.size.width * factor).roundToInt(), (info.size.height * factor).roundToInt())
        }.copy(Bitmap.Config.ARGB_8888, false)

    private companion object {
        const val SEGMENTER_FILE = "u2netp.onnx"
        const val INPAINTER_FILE = "migan_pipeline_v2.onnx"
        const val PHOTO_SIDE = 2400
        const val WORKING_SIDE = 1280
        const val OUTPUT_SIDE = 720
        const val INSET = 0.015
    }
}

private class Clock {
    private val started = SystemClock.elapsedRealtime()
    private var last = started

    fun lap(): Long = SystemClock.elapsedRealtime().let { now -> (now - last).also { last = now } }

    fun total(): Long = SystemClock.elapsedRealtime() - started
}

internal fun Bitmap.pixels(): IntArray = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

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

private fun Bitmap.overlay(subject: BooleanArray, quad: Quad?): Bitmap {
    val pixels = pixels()
    for (i in pixels.indices) if (!subject[i]) pixels[i] = Color.rgb(Color.red(pixels[i]) * 35 / 100, Color.green(pixels[i]) * 35 / 100, Color.blue(pixels[i]) * 35 / 100)
    val out = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
    quad ?: return out
    val path = Path().apply {
        moveTo(quad.x(0).toFloat(), quad.y(0).toFloat())
        for (i in 1..3) lineTo(quad.x(i).toFloat(), quad.y(i).toFloat())
        close()
    }
    Canvas(out).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(width, height) / 200f; color = QUAD_COLOR })
    return out
}

private fun Bitmap.tinted(repaint: BooleanArray): Bitmap {
    val pixels = pixels()
    for (i in pixels.indices) if (repaint[i]) {
        val p = pixels[i]
        pixels[i] = Color.rgb((Color.red(p) * 4 + 255 * 6) / 10, Color.green(p) * 4 / 10, (Color.blue(p) * 4 + 200 * 6) / 10)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private val QUAD_COLOR = Color.rgb(0, 255, 80)
