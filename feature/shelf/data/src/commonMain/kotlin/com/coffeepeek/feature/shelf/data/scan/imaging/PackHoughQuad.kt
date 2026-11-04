package com.coffeepeek.feature.shelf.data.scan.imaging

import com.coffeepeek.feature.shelf.data.scan.geometry.dilate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Shape prior for the bag: a quad made of four straight edge lines. Faces, cups and hands merged into the
 * saliency mask have no long straight sides, so this start often beats the hull-based ones. Lines come from
 * a Hough transform of the mask outline plus photo edges near it, split into two families by the mask's
 * orientation [baseAngle]; every 2 + 2 combination is scored by coverage, background and edge support.
 */
internal fun houghQuad(mask: BooleanArray, maskWidth: Int, maskHeight: Int, gray: Plane, baseAngle: Double): Quad? {
    val scale = HOUGH_GRID.toDouble() / max(maskWidth, maskHeight)
    val width = (maskWidth * scale).roundToInt()
    val height = (maskHeight * scale).roundToInt()
    val small = BooleanArray(width * height) {
        mask[min(maskHeight - 1, ((it / width + 0.5) / scale).toInt()) * maskWidth + min(maskWidth - 1, ((it % width + 0.5) / scale).toInt())]
    }
    val band = dilate(small, width, 3)
    val photo = gray.resizeArea(width, height).canny(50f, 150f)
    val gradient = morphologicalGradient(small, width)
    val outline = BooleanArray(small.size) { gradient[it] || (photo[it] && band[it]) }
    val lines = houghLines(outline, width, height, 0.2 * min(width, height))
    val families = lines.groupBy { familyOf(it, baseAngle) }
    val a = dedupe(families[0].orEmpty(), 0.08 * min(width, height))
    val b = dedupe(families[1].orEmpty(), 0.08 * min(width, height))
    if (a.size < 2 || b.size < 2) return null
    return CombinationScorer(small, dilate(outline, width, 1), width, height).best(a, b)?.scaled(1 / scale)
}

/** Line x cos(theta) + y sin(theta) = rho with its vote count (≈ edge pixels on it). */
internal class HoughLine(val theta: Double, val rho: Double, val votes: Int)

private fun houghLines(edges: BooleanArray, width: Int, height: Int, minVotes: Double): List<HoughLine> {
    val diagonal = hypot(width.toDouble(), height.toDouble()).toInt() + 1
    val rhos = 2 * diagonal + 1
    val accumulator = IntArray(ANGLES * rhos)
    val cosines = DoubleArray(ANGLES) { cos(it * PI / ANGLES) }
    val sines = DoubleArray(ANGLES) { sin(it * PI / ANGLES) }
    for (i in edges.indices) {
        if (!edges[i]) continue
        val x = i % width
        val y = i / width
        for (t in 0 until ANGLES) accumulator[t * rhos + (x * cosines[t] + y * sines[t]).roundToInt() + diagonal]++
    }
    return peaks(accumulator, rhos, minVotes).map { HoughLine(it / rhos * PI / ANGLES, (it % rhos - diagonal).toDouble(), accumulator[it]) }
}

// Local maxima above [minVotes] in a 5x5 (angle, rho) window, strongest first.
private fun peaks(accumulator: IntArray, rhos: Int, minVotes: Double): List<Int> {
    val found = accumulator.indices.filter { i ->
        val votes = accumulator[i]
        if (votes < minVotes) return@filter false
        val t = i / rhos
        val r = i % rhos
        (-2..2).all { dt -> (-2..2).all { dr -> isNotStronger(accumulator, rhos, t + dt, r + dr, votes, dt == 0 && dr == 0, dt < 0 || (dt == 0 && dr < 0)) } }
    }
    return found.sortedByDescending { accumulator[it] }.take(MAX_PEAKS)
}

private fun isNotStronger(accumulator: IntArray, rhos: Int, t: Int, r: Int, votes: Int, self: Boolean, before: Boolean): Boolean {
    if (self || t !in 0 until ANGLES || r !in 0 until rhos) return true
    val other = accumulator[t * rhos + r]
    return if (before) other < votes else other <= votes
}

// 0: along the mask's main axis, 1: across it, null: neither (within 25 degrees).
private fun familyOf(line: HoughLine, baseAngle: Double): Int? {
    val direction = line.theta + PI / 2
    val delta = ((direction - baseAngle) % PI + PI) % PI
    return when {
        min(delta, PI - delta) < TOLERANCE -> 0
        abs(delta - PI / 2) < TOLERANCE -> 1
        else -> null
    }
}

/** Keeps the strongest line among near-parallel neighbours with a similar offset. */
private fun dedupe(lines: List<HoughLine>, minGap: Double): List<HoughLine> {
    val kept = mutableListOf<HoughLine>()
    for (line in lines.sortedByDescending { it.votes }) {
        val separate = kept.all {
            val dot = cos(line.theta) * cos(it.theta) + sin(line.theta) * sin(it.theta)
            abs(line.rho - it.rho * sign(dot)) > minGap
        }
        if (separate) kept.add(line)
        if (kept.size == MAX_PER_FAMILY) break
    }
    return kept
}

private class CombinationScorer(val mask: BooleanArray, val support: BooleanArray, val width: Int, val height: Int) {
    private val total = max(mask.count { it }, 1)

    fun best(a: List<HoughLine>, b: List<HoughLine>): Quad? {
        var best: Quad? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (pairA in pairs(a)) for (pairB in pairs(b)) {
            val corners = pairA.flatMap { p -> pairB.map { q -> intersection(p, q) } }
            if (corners.any { it == null }) continue
            val quad = Quad.ordered(corners.filterNotNull())
            if (!quad.isConvex()) continue
            val score = score(quad)
            if (score > bestScore) {
                best = quad
                bestScore = score
            }
        }
        return best
    }

    private fun score(quad: Quad): Double {
        var masked = 0
        var pixels = 0
        for (y in 0 until height) {
            val span = quad.rowSpan(y, width) ?: continue
            for (x in span) if (mask[y * width + x]) masked++
            pixels += span.last - span.first + 1
        }
        return (masked - 2.0 * (pixels - masked)) / total + 0.5 * perimeterSupport(quad)
    }

    private fun perimeterSupport(quad: Quad): Double {
        var on = 0
        var all = 0
        for (side in 0 until 4) {
            val steps = max(1, quad.sideLength(side).toInt())
            for (k in 0..steps) {
                val x = (quad.x(side) + (quad.x(side + 1) - quad.x(side)) * k / steps).roundToInt()
                val y = (quad.y(side) + (quad.y(side + 1) - quad.y(side)) * k / steps).roundToInt()
                if (x !in 0 until width || y !in 0 until height) continue
                all++
                if (support[y * width + x]) on++
            }
        }
        return on.toDouble() / max(all, 1)
    }
}

private fun pairs(lines: List<HoughLine>): List<List<HoughLine>> =
    lines.indices.flatMap { i -> (i + 1 until lines.size).map { j -> listOf(lines[i], lines[j]) } }

private fun intersection(p: HoughLine, q: HoughLine): Pair<Double, Double>? {
    val a1 = cos(p.theta)
    val b1 = sin(p.theta)
    val a2 = cos(q.theta)
    val b2 = sin(q.theta)
    val det = a1 * b2 - a2 * b1
    if (abs(det) < 1e-6) return null
    return (p.rho * b2 - q.rho * b1) / det to (a1 * q.rho - a2 * p.rho) / det
}

private const val HOUGH_GRID = 256
private const val ANGLES = 180
private const val MAX_PEAKS = 40
private const val MAX_PER_FAMILY = 8
private val TOLERANCE = radians(25.0)
