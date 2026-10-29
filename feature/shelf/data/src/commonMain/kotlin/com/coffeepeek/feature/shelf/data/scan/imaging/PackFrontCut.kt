package com.coffeepeek.feature.shelf.data.scan.imaging

import com.coffeepeek.feature.shelf.data.scan.geometry.PackMask
import com.coffeepeek.feature.shelf.data.scan.geometry.convexHull
import com.coffeepeek.feature.shelf.data.scan.geometry.dilate
import kotlin.math.max
import kotlin.math.roundToInt

/** Keeps the largest 4-connected subject region (the bag; stray salient bits elsewhere are dropped). */
internal fun largestRegion(mask: BooleanArray, width: Int): BooleanArray {
    val parts = components(mask, width, eightConnected = false)
    if (parts.count <= 1) return BooleanArray(mask.size)
    val largest = (1 until parts.count).maxBy { parts.areas[it] }
    return BooleanArray(mask.size) { parts.labels[it] == largest }
}

/** The chosen bag front: [quad] in mask pixels (before the inset) and the scorer that judged it. */
internal class FrontFit(val quad: Quad, val scorer: QuadScorer, val start: String)

/**
 * Several starting quads (hull diagonals, minimum-area box, Hough lines, 4-vertex polygon) are refined
 * by local search; the best score wins. Null when the mask is empty.
 */
internal fun fitFront(subject: BooleanArray, width: Int, height: Int, gray: Plane): FrontFit? {
    val hull = PackMask(width, height, FloatArray(subject.size) { if (subject[it]) 1f else 0f }).convexHull()
        .map { it.x.toDouble() to it.y.toDouble() }
    if (hull.size < 3) return null
    val scorer = QuadScorer(subject, width, height, gray)
    val (box, axis) = minAreaBox(hull)
    val starts = buildMap {
        put("hull", diagonalQuad(hull))
        put("box", box)
        houghQuad(subject, width, height, gray, axis)?.let { put("hough", it) }
        fourVertexPolygon(hull)?.let { put("poly", it) }
    }
    val (name, best) = starts.mapValues { scorer.refine(it.value.scaled(scorer.scale)) }.maxBy { it.value.second }
    if (best.second == Double.NEGATIVE_INFINITY) return null
    return FrontFit(best.first.scaled(1 / scorer.scale), scorer, name)
}

/** Projective map from the upright [width] x [height] rectangle onto [quad] (TL, TR, BR, BL). */
internal class Homography(quad: Quad, width: Double, height: Double) {
    private val h: DoubleArray

    init {
        val source = listOf(0.0 to 0.0, width to 0.0, width to height, 0.0 to height)
        val rows = Array(8) { DoubleArray(9) }
        for (i in 0 until 4) {
            val (u, v) = source[i]
            val x = quad.x(i)
            val y = quad.y(i)
            rows[2 * i] = doubleArrayOf(u, v, 1.0, 0.0, 0.0, 0.0, -u * x, -v * x, x)
            rows[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, u, v, 1.0, -u * y, -v * y, y)
        }
        h = solve(rows)
    }

    fun map(u: Double, v: Double): Pair<Double, Double> {
        val w = h[6] * u + h[7] * v + 1.0
        return (h[0] * u + h[1] * v + h[2]) / w to (h[3] * u + h[4] * v + h[5]) / w
    }
}

// Gauss-Jordan elimination with partial pivoting of an 8x9 augmented matrix.
private fun solve(rows: Array<DoubleArray>): DoubleArray {
    for (col in 0 until 8) {
        val pivot = (col until 8).maxBy { kotlin.math.abs(rows[it][col]) }
        rows[col] = rows[pivot].also { rows[pivot] = rows[col] }
        val p = rows[col][col]
        for (k in col..8) rows[col][k] /= p
        for (r in 0 until 8) if (r != col) {
            val factor = rows[r][col]
            for (k in col..8) rows[r][k] -= factor * rows[col][k]
        }
    }
    return DoubleArray(8) { rows[it][8] }
}

/** Output size keeping the average lengths of opposite sides, scaled so the long side is [maxSide]. */
internal fun Quad.outputSize(maxSide: Int): Pair<Int, Int> {
    val (w, h) = rectangle()
    val k = maxSide / max(w, h)
    return max(1, (w * k).roundToInt()) to max(1, (h * k).roundToInt())
}

/**
 * Pixels of the rectified [outWidth] x [outHeight] rectangle that the photo cannot supply: outside the
 * photo or not part of the subject. They are repainted, so the result is always a full rectangle.
 */
internal fun missingAfterRectify(subject: BooleanArray, width: Int, height: Int, quad: Quad, outWidth: Int, outHeight: Int): BooleanArray {
    val homography = Homography(quad, outWidth.toDouble(), outHeight.toDouble())
    return BooleanArray(outWidth * outHeight) {
        val (x, y) = homography.map(it % outWidth + 0.5, it / outWidth + 0.5)
        val px = x.toInt()
        val py = y.toInt()
        x < 0 || y < 0 || px >= width || py >= height || !subject[py * width + px]
    }
}

/** Skin-coloured blobs touching the rectangle border (fingers holding the bag), up to [maxShare] of it. */
internal fun borderSkin(pixels: IntArray, width: Int, height: Int, maxShare: Float = MAX_SKIN): BooleanArray {
    val skin = BooleanArray(pixels.size) { isSkin(pixels[it]) }
    val opened = dilate(erode(skin, width, 2), width, 2)
    val parts = components(opened, width, eightConnected = true)
    val keep = BooleanArray(parts.count) { label ->
        val box = parts.boxes[label]
        label > 0 && parts.areas[label] <= maxShare * pixels.size &&
            (box[0] == 0 || box[1] == 0 || box[2] == width || box[3] == height)
    }
    return BooleanArray(pixels.size) { keep[parts.labels[it]] }
}

// YCrCb skin range (Y 40..245, Cr 136..173, Cb 77..127) with OpenCV's RGB to YCrCb coefficients.
private fun isSkin(pixel: Int): Boolean {
    val r = pixel shr 16 and 0xFF
    val g = pixel shr 8 and 0xFF
    val b = pixel and 0xFF
    val y = 0.299 * r + 0.587 * g + 0.114 * b
    val cr = (r - y) * 0.713 + 128
    val cb = (b - y) * 0.564 + 128
    return y in 40.0..245.0 && cr in 136.0..173.0 && cb in 77.0..127.0
}

/** Everything to repaint in the rectified bag: missing pixels and border fingers, widened by [rim]. */
internal fun repaintMask(missing: BooleanArray, rectified: IntArray, width: Int, height: Int, rim: Int = RIM): BooleanArray {
    val fingers = borderSkin(rectified, width, height)
    return dilate(BooleanArray(missing.size) { missing[it] || fingers[it] }, width, rim)
}

private const val MAX_SKIN = 0.08f
private const val RIM = 4
