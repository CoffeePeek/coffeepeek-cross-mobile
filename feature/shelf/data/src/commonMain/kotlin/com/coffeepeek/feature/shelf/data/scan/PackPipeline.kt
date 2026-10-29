package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.data.scan.imaging.*
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.usecase.PackNotFound
import com.coffeepeek.feature.shelf.domain.usecase.moderate
import kotlin.time.TimeSource

/** Upright ARGB pixels, row by row. */
internal class PackImage(val width: Int, val height: Int, val pixels: IntArray)

/** What the platform's graphics stack does for the scan: decoding, scaling, the perspective stretch and JPEG. */
internal interface PackImaging {
    /** The photo upright, at most [maxSide] on its longer side; [BagScanFailure] with UnreadablePhoto when it cannot be read. */
    fun decode(photo: ByteArray, maxSide: Int): PackImage

    /** [image] scaled down so its longer side is at most [maxSide]; a smaller image is returned as is. */
    fun scaledDown(image: PackImage, maxSide: Int): PackImage

    /** [quad] of [image] stretched onto an opaque upright [width] x [height] rectangle; pixels outside the photo are black. */
    fun rectified(image: PackImage, quad: Quad, width: Int, height: Int): PackImage

    fun jpeg(image: PackImage, quality: Int): ByteArray
}

/** The two networks. Each call opens its model for that call only, so at most one of them is in memory. */
internal interface PackNetworks {
    /** U2-Net-p on a normalised [1, 3, SEGMENTER_SIDE, SEGMENTER_SIDE] input; its first output. */
    fun segment(input: FloatArray): FloatArray

    /** MI-GAN's pipeline on uint8 CHW [image] and [known] (255 keeps a pixel); uint8 CHW out. ModelMissing while not downloaded. */
    fun inpaint(image: ByteArray, known: ByteArray, width: Int, height: Int): ByteArray
}

/**
 * Photo -> U2-Net-p subject -> refined front quad -> upright full rectangle with MI-GAN repair -> gentle shading/glare
 * softening -> JPEG card, plus pre-moderation. Each model is opened only for its stage, which keeps the peak low.
 */
internal class PackPipeline(private val imaging: PackImaging, private val networks: PackNetworks) {
    fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): BagScanResult {
        val clock = Clock()
        val full = imaging.decode(photo, PHOTO_SIDE)
        val working = imaging.scaledDown(full, WORKING_SIDE)
        val rgb = RgbPlanes.of(working.pixels, working.width, working.height)
        val probability = networks.subjectProbability(rgb)
        val subject = largestRegion(BooleanArray(probability.data.size) { probability.data[it] > 0.5f }, working.width)
        val segmentation = clock.lap()
        onStage(BagScanStage.Segmentation)
        val front = fitFront(subject, working.width, working.height, rgb.gray())
        val fit = clock.lap()
        onStage(BagScanStage.Front)
        if (front == null) return BagScanResult(null, 0, 0, null, null, PackNotFound, BagScanTimings(segmentation, fit, 0, 0, clock.total()))
        onFront(front.quad.inset(INSET).frame(working))
        return finish(Scan(full, working, subject, front, clock, segmentation, fit), onStage)
    }

    private fun finish(scan: Scan, onStage: (BagScanStage) -> Unit): BagScanResult {
        val working = scan.working
        val quad = scan.front.quad.inset(INSET)
        val (width, height) = quad.outputSize(OUTPUT_SIDE)
        val rectified = imaging.rectified(scan.full, quad.scaled(scan.full.width.toDouble() / working.width), width, height)
        val missing = missingAfterRectify(scan.subject, working.width, working.height, quad, width, height)
        val repaint = repaintMask(missing, rectified.pixels, width, height)
        val repaired = if (repaint.none { it }) rectified else networks.inpainted(rectified, repaint)
        val repairMillis = scan.clock.lap()
        onStage(BagScanStage.Repair)
        val softened = soften(repaired.pixels, width, height)
        val card = imaging.jpeg(PackImage(width, height, softened.pixels), JPEG_QUALITY)
        val softenMillis = scan.clock.lap()
        onStage(BagScanStage.Soften)
        val quality = frontQuality(scan.front, scan.subject, working.width, working.height)
            .copy(repaintShare = repaint.count { it }.toDouble() / repaint.size, strongGlare = softened.strongGlareShare.toDouble())
        return BagScanResult(ShelfBagCard(card, "image/jpeg", "bag.jpg"), width, height, quad.frame(working), quality, moderate(quality),
            BagScanTimings(scan.segmentation, scan.fit, repairMillis, softenMillis, scan.clock.total()))
    }

    private class Scan(
        val full: PackImage, val working: PackImage, val subject: BooleanArray, val front: FrontFit, val clock: Clock,
        val segmentation: Long, val fit: Long,
    )

    private companion object {
        const val PHOTO_SIDE = 2400
        const val WORKING_SIDE = 1280
        const val OUTPUT_SIDE = 720
        const val INSET = 0.015
        const val JPEG_QUALITY = 85
    }
}

/** Corners as shares of the working image, which has the photo's proportions. */
private fun Quad.frame(working: PackImage) =
    BagScanFrame((0 until 4).map { (x(it) / working.width).toFloat() to (y(it) / working.height).toFloat() })

private class Clock {
    private val started = TimeSource.Monotonic.markNow()
    private var last = started

    fun lap(): Long = TimeSource.Monotonic.markNow().let { now -> (now - last).inWholeMilliseconds.also { last = now } }

    fun total(): Long = started.elapsedNow().inWholeMilliseconds
}
