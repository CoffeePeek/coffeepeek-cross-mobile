package com.coffeepeek.admin.sandbox.packscan.imaging

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Quadrilateral as [x0, y0, x1, y1, x2, y2, x3, y3]: top-left, top-right, bottom-right, bottom-left. */
@JvmInline internal value class Quad(val c: DoubleArray) {
    init {
        require(c.size == 8)
    }

    fun x(i: Int): Double = c[2 * (i and 3)]
    fun y(i: Int): Double = c[2 * (i and 3) + 1]

    fun scaled(factor: Double) = Quad(DoubleArray(8) { c[it] * factor })

    fun moved(corner: Int, dx: Double, dy: Double) = Quad(c.copyOf().also { it[2 * corner] += dx; it[2 * corner + 1] += dy })

    fun sideLength(i: Int): Double = hypot(x(i + 1) - x(i), y(i + 1) - y(i))

    /** Average lengths of opposite sides: the upright rectangle the quad is stretched onto. */
    fun rectangle(): Pair<Double, Double> = (sideLength(0) + sideLength(2)) / 2 to (sideLength(1) + sideLength(3)) / 2

    /** Pulls every corner towards the opposite one by [share] (the blurry mask overshoots the bag). */
    fun inset(share: Double) = Quad(DoubleArray(8) { c[it] + (c[(it + 4) % 8] - c[it]) * share })

    fun isConvex(): Boolean {
        val signs = (0 until 4).map {
            (x(it + 1) - x(it)) * (y(it + 2) - y(it + 1)) - (y(it + 1) - y(it)) * (x(it + 2) - x(it + 1))
        }
        return signs.all { it > 0 } || signs.all { it < 0 }
    }

    /** Largest deviation of an inner angle from 90 degrees. */
    fun angleDeviation(): Double = (0 until 4).maxOf { abs(innerAngle(it) - 90.0) }

    fun innerAngle(i: Int): Double {
        val ax = x(i - 1) - x(i)
        val ay = y(i - 1) - y(i)
        val bx = x(i + 1) - x(i)
        val by = y(i + 1) - y(i)
        val cosine = (ax * bx + ay * by) / max(hypot(ax, ay) * hypot(bx, by), 1e-9)
        return Math.toDegrees(acos(cosine.coerceIn(-1.0, 1.0)))
    }

    /** Pixel columns [first, last] whose centres lie inside on row [y] (pixel centre y + 0.5); null when empty. */
    fun rowSpan(y: Int, width: Int): IntRange? {
        val cy = y + 0.5
        var left = Double.MAX_VALUE
        var right = -Double.MAX_VALUE
        for (i in 0 until 4) {
            val y0 = y(i)
            val y1 = y(i + 1)
            if ((cy < y0 && cy < y1) || (cy > y0 && cy > y1) || y0 == y1) continue
            val x = x(i) + (cy - y0) / (y1 - y0) * (x(i + 1) - x(i))
            left = min(left, x)
            right = max(right, x)
        }
        if (left > right) return null
        val first = max(0, ceil(left - 0.5).toInt())
        val last = min(width - 1, floor(right - 0.5).toInt())
        return if (first <= last) first..last else null
    }

    companion object {
        /** Orders four points as TL, TR, BR, BL by the extremes of x + y and x - y (y grows downwards). */
        fun ordered(points: List<Pair<Double, Double>>): Quad {
            val tl = points.minBy { it.first + it.second }
            val tr = points.maxBy { it.first - it.second }
            val br = points.maxBy { it.first + it.second }
            val bl = points.minBy { it.first - it.second }
            return Quad(doubleArrayOf(tl.first, tl.second, tr.first, tr.second, br.first, br.second, bl.first, bl.second))
        }
    }
}

/** A photographed bag front: no very sharp corners, opposite sides of comparable length. */
internal fun Quad.isPlausible(): Boolean {
    val lengths = (0 until 4).map { sideLength(it) }
    if (lengths.min() < 1e-6) return false
    if (max(lengths[0], lengths[2]) > MAX_OPPOSITE_RATIO * min(lengths[0], lengths[2])) return false
    if (max(lengths[1], lengths[3]) > MAX_OPPOSITE_RATIO * min(lengths[1], lengths[3])) return false
    return (0 until 4).all { abs(cos(Math.toRadians(innerAngle(it)))) <= MAX_CORNER_COS }
}

private const val MAX_OPPOSITE_RATIO = 1.8
private val MAX_CORNER_COS = cos(Math.toRadians(55.0))

/** Corners as the hull extremes along both diagonals. */
internal fun diagonalQuad(hull: List<Pair<Double, Double>>): Quad = Quad.ordered(hull)

/** Minimum-area enclosing rectangle by rotating calipers over the hull edges; also returns its axis angle. */
internal fun minAreaBox(hull: List<Pair<Double, Double>>): Pair<Quad, Double> {
    var best: Pair<Quad, Double>? = null
    var bestArea = Double.MAX_VALUE
    for (i in hull.indices) {
        val (x0, y0) = hull[i]
        val (x1, y1) = hull[(i + 1) % hull.size]
        val length = hypot(x1 - x0, y1 - y0).takeIf { it > 1e-9 } ?: continue
        val ux = (x1 - x0) / length
        val uy = (y1 - y0) / length
        val us = hull.map { it.first * ux + it.second * uy }
        val vs = hull.map { -it.first * uy + it.second * ux }
        val area = (us.max() - us.min()) * (vs.max() - vs.min())
        if (area >= bestArea) continue
        bestArea = area
        val corners = listOf(us.min() to vs.min(), us.max() to vs.min(), us.max() to vs.max(), us.min() to vs.max())
            .map { (u, v) -> u * ux - v * uy to u * uy + v * ux }
        best = Quad.ordered(corners) to kotlin.math.atan2(uy, ux)
    }
    return best ?: (Quad.ordered(hull) to 0.0)
}

/** First Douglas-Peucker simplification of the closed hull with exactly four vertices, if any. */
internal fun fourVertexPolygon(hull: List<Pair<Double, Double>>): Quad? {
    if (hull.size < 4) return null
    val perimeter = hull.indices.sumOf { distance(hull[it], hull[(it + 1) % hull.size]) }
    for (step in 0 until 30) {
        val epsilon = (0.01 + step * (0.14 / 29)) * perimeter
        val polygon = simplifyClosed(hull, epsilon)
        if (polygon.size == 4) return Quad.ordered(polygon)
    }
    return null
}

// Splits the closed curve at two far-apart points and simplifies both chains.
private fun simplifyClosed(points: List<Pair<Double, Double>>, epsilon: Double): List<Pair<Double, Double>> {
    val a = points.indices.maxBy { distance(points[0], points[it]) }
    val b = points.indices.maxBy { distance(points[a], points[it]) }
    val (first, second) = if (a < b) a to b else b to a
    val chainA = points.subList(first, second + 1)
    val chainB = points.subList(second, points.size) + points.subList(0, first + 1)
    return simplify(chainA, epsilon).dropLast(1) + simplify(chainB, epsilon).dropLast(1)
}

private fun simplify(points: List<Pair<Double, Double>>, epsilon: Double): List<Pair<Double, Double>> {
    if (points.size < 3) return points
    val first = points.first()
    val last = points.last()
    val (index, farthest) = (1 until points.size - 1).map { it to lineDistance(points[it], first, last) }.maxBy { it.second }
    if (farthest <= epsilon) return listOf(first, last)
    return simplify(points.subList(0, index + 1), epsilon).dropLast(1) + simplify(points.subList(index, points.size), epsilon)
}

private fun lineDistance(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val length = distance(a, b)
    if (length < 1e-9) return distance(p, a)
    return abs((b.first - a.first) * (a.second - p.second) - (a.first - p.first) * (b.second - a.second)) / length
}

internal fun distance(a: Pair<Double, Double>, b: Pair<Double, Double>): Double = hypot(a.first - b.first, a.second - b.second)
