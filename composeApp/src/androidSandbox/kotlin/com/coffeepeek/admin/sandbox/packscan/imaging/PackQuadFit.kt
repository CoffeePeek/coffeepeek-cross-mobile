package com.coffeepeek.admin.sandbox.packscan.imaging

import com.coffeepeek.admin.sandbox.packscan.geometry.dilate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Scores bag-front quads on a [GRID]-sized copy of the subject mask and the photo edges around it:
 * mask coverage (salient texture such as a heap of beans counts less) minus [PENALTY] x background inside,
 * plus [EDGE_WEIGHT] x how well every side lies on an edge running along it (the weakest side counts double).
 */
internal class QuadScorer(subject: BooleanArray, maskWidth: Int, maskHeight: Int, gray: Plane) {
    val scale: Double = GRID.toDouble() / max(maskWidth, maskHeight)
    val width = (maskWidth * scale).roundToInt()
    val height = (maskHeight * scale).roundToInt()
    val mask: BooleanArray = Plane(maskWidth, maskHeight, FloatArray(subject.size) { if (subject[it]) 1f else 0f })
        .resizeArea(width, height).data.let { area -> BooleanArray(area.size) { area[it] >= 0.5f } }
    val textured: BooleanArray
    private val distances: List<FloatArray>
    private val weightRows: FloatArray
    private val maskRows: IntArray
    private val total: Double

    init {
        val small = gray.resizeArea(width, height).gaussianBlur(sigmaForKernel(3))
        val band = dilate(mask, width, 7)
        val photo = small.canny(40f, 120f).also { for (i in it.indices) it[i] = it[i] && band[i] }
        val outline = morphologicalGradient(mask, width)
        distances = orientedDistances(photo, outline, edgeAngles(small, photo))
        val density = Plane(width, height, FloatArray(photo.size) { if (photo[it]) 1f else 0f }).boxBlur(4)
        textured = BooleanArray(photo.size) { density.data[it] > TEXTURE }
        val weight = FloatArray(mask.size) { if (!mask[it]) 0f else if (textured[it]) TEXTURE_WEIGHT else 1f }
        total = max(weight.sum().toDouble(), 1.0)
        weightRows = rowPrefix(weight)
        maskRows = rowPrefix(mask)
    }

    // Gradient direction mod pi: photo edges from the image, mask outline from the softened mask.
    private fun edgeAngles(gray: Plane, photo: BooleanArray): FloatArray {
        val soft = Plane(width, height, FloatArray(mask.size) { if (mask[it]) 1f else 0f }).gaussianBlur(sigmaForKernel(5))
        val (px, py) = gray.sobel()
        val (mx, my) = soft.sobel()
        return FloatArray(mask.size) {
            val angle = if (photo[it]) atan2(py.data[it], px.data[it]) else atan2(my.data[it], mx.data[it])
            ((angle % PI.toFloat()) + PI.toFloat()) % PI.toFloat()
        }
    }

    private fun orientedDistances(photo: BooleanArray, outline: BooleanArray, angle: FloatArray): List<FloatArray> {
        val bins = IntArray(angle.size) { (angle[it] / PI * BINS).toInt() % BINS }
        return (0 until BINS).map { bin ->
            val near = BooleanArray(angle.size) {
                val gap = Math.floorMod(bins[it] - bin, BINS)
                (photo[it] || outline[it]) && (gap <= 1 || gap >= BINS - 1)
            }
            distanceTo(near, width, height)
        }
    }

    /** Pixels counted inside [quad]: (coverage weight, mask pixels, all pixels). */
    fun coverage(quad: Quad): Triple<Double, Int, Int> {
        var weight = 0.0
        var masked = 0
        var pixels = 0
        for (y in 0 until height) {
            val span = quad.rowSpan(y, width) ?: continue
            val row = y * (width + 1)
            weight += weightRows[row + span.last + 1] - weightRows[row + span.first]
            masked += maskRows[row + span.last + 1] - maskRows[row + span.first]
            pixels += span.last - span.first + 1
        }
        return Triple(weight, masked, pixels)
    }

    /** Share of side samples lying on an edge that runs along the side (0..1). */
    fun sideSupport(quad: Quad, side: Int): Double {
        val ax = quad.x(side)
        val ay = quad.y(side)
        val bx = quad.x(side + 1)
        val by = quad.y(side + 1)
        val normal = ((atan2(ax - bx, by - ay) % PI) + PI) % PI
        val distance = distances[(normal / PI * BINS).roundToInt() % BINS]
        return (0 until SAMPLES).sumOf { k ->
            val t = 0.04 + 0.92 * k / (SAMPLES - 1)
            val x = (ax + (bx - ax) * t).roundToInt()
            val y = (ay + (by - ay) * t).roundToInt()
            if (x !in 0 until width || y !in 0 until height) 0.5 else (1.0 - distance[y * width + x] / 3.0).coerceIn(0.0, 1.0)
        } / SAMPLES
    }

    fun supports(quad: Quad): List<Double> = (0 until 4).map { sideSupport(quad, it) }

    fun score(quad: Quad): Double {
        if (!quad.isConvex() || !quad.isPlausible()) return Double.NEGATIVE_INFINITY
        val (weight, masked, pixels) = coverage(quad)
        val sides = supports(quad)
        val support = (sides.sum() + sides.min()) / 5
        return (weight - PENALTY * (pixels - masked)) / total + EDGE_WEIGHT * support
    }

    /** Greedy local search over corner and side moves with shrinking steps; [start] in grid pixels. */
    fun refine(start: Quad): Pair<Quad, Double> {
        var best = start
        var bestScore = score(start)
        for (step in STEPS) {
            for (iteration in 0 until MAX_ITERATIONS) {
                val (candidate, candidateScore) = moves(best, step).map { it to score(it) }.maxBy { it.second }
                if (candidateScore <= bestScore + 1e-6) break
                best = candidate
                bestScore = candidateScore
            }
        }
        return best to bestScore
    }

    // Every corner in 8 directions and every side along its normal, by [step].
    private fun moves(quad: Quad, step: Double): List<Quad> = buildList {
        for (corner in 0 until 4) {
            for ((dx, dy) in DIRECTIONS) add(quad.moved(corner, dx * step, dy * step))
            val ax = quad.x(corner)
            val ay = quad.y(corner)
            val length = max(hypot(quad.x(corner + 1) - ax, quad.y(corner + 1) - ay), 1e-9)
            val nx = (quad.y(corner + 1) - ay) / length * step
            val ny = (ax - quad.x(corner + 1)) / length * step
            for (sign in intArrayOf(1, -1)) add(quad.moved(corner, sign * nx, sign * ny).moved((corner + 1) % 4, sign * nx, sign * ny))
        }
    }

    private fun rowPrefix(values: FloatArray): FloatArray {
        val out = FloatArray((width + 1) * height)
        for (y in 0 until height) for (x in 0 until width) out[y * (width + 1) + x + 1] = out[y * (width + 1) + x] + values[y * width + x]
        return out
    }

    private fun rowPrefix(values: BooleanArray): IntArray {
        val out = IntArray((width + 1) * height)
        for (y in 0 until height) for (x in 0 until width) out[y * (width + 1) + x + 1] = out[y * (width + 1) + x] + if (values[y * width + x]) 1 else 0
        return out
    }

    private companion object {
        const val GRID = 384
        const val PENALTY = 2.0
        const val EDGE_WEIGHT = 0.6
        const val BINS = 12
        const val SAMPLES = 48
        const val TEXTURE = 0.3f
        const val TEXTURE_WEIGHT = 0.25f
        const val MAX_ITERATIONS = 60
        val STEPS = doubleArrayOf(12.0, 6.0, 3.0, 1.5, 0.75)
        val DIRECTIONS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
    }
}

/** Outline pixels: dilation minus erosion by one pixel. */
internal fun morphologicalGradient(mask: BooleanArray, width: Int): BooleanArray {
    val grown = dilate(mask, width, 1)
    val shrunk = erode(mask, width, 1)
    return BooleanArray(mask.size) { grown[it] && !shrunk[it] }
}
