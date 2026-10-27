package com.coffeepeek.feature.shelf.data.scan.imaging

import com.coffeepeek.feature.shelf.domain.model.PackModerationStatus
import com.coffeepeek.feature.shelf.domain.usecase.moderate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.*

class PackImagingTest {
    @Test fun labRoundTripKeepsColoursAndWhiteIsL100() {
        val lab = FloatArray(3)
        rgbToLab(255f, 255f, 255f, lab)
        assertEquals(100f, lab[0], 0.1f)
        val rgb = FloatArray(3)
        for (color in listOf(Triple(240f, 180f, 20f), Triple(20f, 30f, 35f), Triple(90f, 140f, 210f))) {
            rgbToLab(color.first, color.second, color.third, lab)
            labToRgb(lab[0], lab[1], lab[2], rgb)
            assertEquals(color.first, rgb[0], 0.5f)
            assertEquals(color.second, rgb[1], 0.5f)
            assertEquals(color.third, rgb[2], 0.5f)
        }
    }

    @Test fun chamferDistanceGrowsAwayFromTheSetPixel() {
        val set = BooleanArray(25).also { it[12] = true }
        val distance = distanceTo(set, 5, 5)
        assertEquals(0f, distance[12])
        assertEquals(0.955f, distance[13], 1e-4f)
        assertEquals(1.3693f, distance[18], 1e-4f)
        assertEquals(1.91f, distance[14], 1e-3f)
    }

    @Test fun componentsReportAreaAndBoxes() {
        val mask = cells("##..", "#...", "...#")
        val parts = components(mask, 4, eightConnected = false)
        assertEquals(3, parts.count)
        assertEquals(listOf(0, 3, 1), parts.areas.toList())
        assertEquals(listOf(3, 2, 4, 3), parts.boxes[2].toList())
    }

    @Test fun cannyFindsTheBorderOfABrightSquare() {
        val plane = Plane(20, 20, FloatArray(400) { if (it % 20 in 5..14 && it / 20 in 5..14) 200f else 20f })
        val edges = plane.canny(40f, 120f)
        assertTrue(edges.count { it } in 30..80)
        assertFalse(edges[10 * 20 + 10])
    }

    @Test fun fitFrontRecoversARotatedBagFromItsMaskAndPhoto() {
        val truth = rotatedRectangle(cx = 200.0, cy = 260.0, w = 220.0, h = 300.0, degrees = 8.0)
        val (subject, gray) = scene(truth, 400, 520)
        val front = assertNotNull(fitFront(subject, 400, 520, gray))
        for (i in 0 until 4) {
            assertTrue(abs(front.quad.x(i) - truth.x(i)) < 6 && abs(front.quad.y(i) - truth.y(i)) < 6, "corner $i: ${front.quad.c.toList()}")
        }
        val quality = frontQuality(front, subject, 400, 520)
        assertTrue(quality.sideSupport > 0.8, "support ${quality.sideSupport}")
        assertEquals(PackModerationStatus.Accepted, moderate(quality).status)
    }

    @Test fun rectifyingThePhotoQuadMarksOnlyOutsidePixelsAsMissing() {
        val truth = rotatedRectangle(cx = 100.0, cy = 100.0, w = 120.0, h = 80.0, degrees = 0.0)
        val (subject) = scene(truth, 200, 200)
        val missing = missingAfterRectify(subject, 200, 200, truth.inset(0.02), 60, 40)
        assertEquals(0, missing.count { it })
        val wider = Quad(doubleArrayOf(-20.0, 60.0, 160.0, 60.0, 160.0, 140.0, -20.0, 140.0))
        assertTrue(missingAfterRectify(subject, 200, 200, wider, 90, 40).count { it } > 0)
    }

    @Test fun implausibleQuadsAreRejected() {
        assertTrue(rotatedRectangle(0.0, 0.0, 100.0, 140.0, 20.0).isPlausible())
        assertFalse(Quad(doubleArrayOf(0.0, 0.0, 100.0, 0.0, 100.0, 100.0, 90.0, 10.0)).isPlausible())
        assertFalse(Quad(doubleArrayOf(0.0, 0.0, 100.0, 0.0, 100.0, 300.0, 0.0, 100.0)).isPlausible())
    }

    @Test fun softeningFlattensBroadShadingButKeepsFineTexture() {
        val width = 240
        val height = 240
        // Yellow ink lit from the left (broad falloff) with fine creases every 6 px.
        val pixels = IntArray(width * height) {
            val x = it % width
            val light = 0.65f + 0.35f * x / width + if (x % 6 == 0) -0.08f else 0f
            argb(235f * light, 185f * light, 30f * light)
        }
        val soft = soften(pixels, width, height).pixels
        fun red(x: Int, y: Int, source: IntArray) = (source[y * width + x] shr 16 and 0xFF).toFloat()
        val broadBefore = red(230, 120, pixels) - red(10, 120, pixels)
        val broadAfter = red(230, 120, soft) - red(10, 120, soft)
        assertTrue(abs(broadAfter) < abs(broadBefore) * 0.6f, "broad $broadBefore -> $broadAfter")
        val creaseBefore = red(121, 120, pixels) - red(120, 120, pixels)
        val creaseAfter = red(121, 120, soft) - red(120, 120, soft)
        assertTrue(creaseAfter > creaseBefore * 0.5f, "crease $creaseBefore -> $creaseAfter")
    }

    private fun rotatedRectangle(cx: Double, cy: Double, w: Double, h: Double, degrees: Double): Quad {
        val a = Math.toRadians(degrees)
        val corners = listOf(-w / 2 to -h / 2, w / 2 to -h / 2, w / 2 to h / 2, -w / 2 to h / 2)
        return Quad(corners.flatMap { (x, y) -> listOf(cx + x * cos(a) - y * sin(a), cy + x * sin(a) + y * cos(a)) }.toDoubleArray())
    }

    // Subject mask and a gray photo: dark bag with a bright label on a light background.
    private fun scene(quad: Quad, width: Int, height: Int): Pair<BooleanArray, Plane> {
        val subject = BooleanArray(width * height)
        for (y in 0 until height) quad.rowSpan(y, width)?.forEach { subject[y * width + it] = true }
        val gray = Plane(width, height, FloatArray(subject.size) { if (subject[it]) 60f else 210f })
        return subject to gray
    }

    private fun cells(vararg rows: String) = rows.joinToString("").map { it == '#' }.toBooleanArray()
}
