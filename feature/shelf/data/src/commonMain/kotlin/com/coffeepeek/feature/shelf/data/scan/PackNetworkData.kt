package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.data.scan.geometry.extendOutward
import com.coffeepeek.feature.shelf.data.scan.imaging.Plane
import com.coffeepeek.feature.shelf.data.scan.imaging.RgbPlanes
import com.coffeepeek.feature.shelf.data.scan.imaging.resizeArea
import com.coffeepeek.feature.shelf.data.scan.imaging.resizeBilinear

/** U2-Net-p's square input side. */
internal const val SEGMENTER_SIDE = 320

private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
private const val ALPHA = 0xFF000000.toInt()

/**
 * U2-Net-p salient-object segmentation (rembg's u2netp.onnx, Apache-2.0, 4.5 MB) with rembg's preprocessing: a 320 px input
 * scaled by its maximum and ImageNet-normalised; the output is min-max normalised. Subject probability (0..1) for every pixel.
 */
internal fun PackNetworks.subjectProbability(image: RgbPlanes): Plane {
    val input = image.resizeArea(SEGMENTER_SIDE, SEGMENTER_SIDE)
    val peak = maxOf(input.r.data.max(), input.g.data.max(), input.b.data.max(), 1e-6f)
    val plane = SEGMENTER_SIDE * SEGMENTER_SIDE
    val tensor = FloatArray(3 * plane)
    listOf(input.r, input.g, input.b).forEachIndexed { c, channel ->
        for (i in 0 until plane) tensor[c * plane + i] = (channel.data[i] / peak - MEAN[c]) / STD[c]
    }
    val raw = segment(tensor)
    val low = raw.min()
    val range = maxOf(raw.max() - low, 1e-6f)
    return Plane(SEGMENTER_SIDE, SEGMENTER_SIDE, FloatArray(raw.size) { (raw[it] - low) / range }).resizeBilinear(image.width, image.height)
}

/**
 * MI-GAN 512 Places2 pipeline (Picsart AI Research, MIT code and weights): it crops around the mask, inpaints at 512 px and
 * blends back, so any image size can be passed. Regenerates [repaint] pixels of [image]; the alpha (bag outline) is kept.
 */
internal fun PackNetworks.inpainted(image: PackImage, repaint: BooleanArray): PackImage {
    val pixels = image.pixels
    val plane = pixels.size
    val inside = BooleanArray(plane) { (pixels[it] ushr 24) > 127 }
    val context = pixels.copyOf().also { extendOutward(it, inside, image.width) }
    val channels = ByteArray(3 * plane)
    for ((c, shift) in intArrayOf(16, 8, 0).withIndex()) for (i in 0 until plane) channels[c * plane + i] = (context[i] shr shift).toByte()
    val known = ByteArray(plane) { if (repaint[it]) 0 else -1 } // -1 is 255 as uint8
    val out = inpaint(channels, known, image.width, image.height)
    return PackImage(image.width, image.height, IntArray(plane) {
        (pixels[it] and ALPHA) or (out.channel(it) shl 16) or (out.channel(plane + it) shl 8) or out.channel(2 * plane + it)
    })
}

private fun ByteArray.channel(index: Int): Int = get(index).toInt() and 0xFF
