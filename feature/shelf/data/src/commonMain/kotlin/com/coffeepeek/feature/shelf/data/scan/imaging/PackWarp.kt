package com.coffeepeek.feature.shelf.data.scan.imaging

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

private const val MAX_GRID = 4

/**
 * [quad] of the [width] x [height] ARGB [pixels] stretched onto an opaque upright [outWidth] x [outHeight] rectangle. Each
 * target pixel averages a grid of bilinear samples about as dense as the photo behind it, so a large photo shrinks without
 * aliasing, as Android's draw-then-downscale does; samples outside the photo are black.
 */
internal fun rectify(pixels: IntArray, width: Int, height: Int, quad: Quad, outWidth: Int, outHeight: Int): IntArray {
    val homography = Homography(quad, outWidth.toDouble(), outHeight.toDouble())
    val (sourceWidth, sourceHeight) = quad.rectangle()
    val grid = ceil(max(sourceWidth / outWidth, sourceHeight / outHeight)).toInt().coerceIn(1, MAX_GRID)
    val samples = (grid * grid).toFloat()
    val sum = FloatArray(3)
    return IntArray(outWidth * outHeight) { index ->
        sum.fill(0f)
        for (sy in 0 until grid) for (sx in 0 until grid) {
            val (x, y) = homography.map(index % outWidth + (sx + 0.5) / grid, index / outWidth + (sy + 0.5) / grid)
            addBilinear(pixels, width, height, x - 0.5, y - 0.5, sum)
        }
        argb(sum[0] / samples, sum[1] / samples, sum[2] / samples)
    }
}

/** Adds the bilinear colour at pixel-grid position ([x], [y]) to [sum]; neighbours outside the image count as black. */
private fun addBilinear(pixels: IntArray, width: Int, height: Int, x: Double, y: Double, sum: FloatArray) {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val fx = (x - x0).toFloat()
    val fy = (y - y0).toFloat()
    for (dy in 0..1) for (dx in 0..1) {
        val px = x0 + dx
        val py = y0 + dy
        if (px < 0 || py < 0 || px >= width || py >= height) continue
        val weight = (if (dx == 0) 1 - fx else fx) * (if (dy == 0) 1 - fy else fy)
        val pixel = pixels[py * width + px]
        sum[0] += weight * (pixel shr 16 and 0xFF)
        sum[1] += weight * (pixel shr 8 and 0xFF)
        sum[2] += weight * (pixel and 0xFF)
    }
}
