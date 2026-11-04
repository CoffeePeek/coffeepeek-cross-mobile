package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.data.scan.imaging.Quad
import com.coffeepeek.feature.shelf.data.scan.imaging.rectify
import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.domain.usecase.PackNotFound
import kotlin.math.abs
import kotlin.test.*

class PackPipelineTest {
    @Test fun aBagOnATableBecomesACardWithItsFrameAndAllStages() {
        val stages = mutableListOf<BagScanStage>()
        var frame: List<Pair<Float, Float>>? = null
        val result = PackPipeline(FakeImaging(bagPhoto()), SubjectIsBright()).scan(byteArrayOf(1), { stages += it }, { frame = it.points })
        assertEquals<List<BagScanStage>>(BagScanStage.entries, stages)
        assertNotNull(result.card)
        assertNotNull(result.quality)
        assertTrue(result.cardHeight > result.cardWidth, "a tall bag stays tall: ${result.cardWidth}x${result.cardHeight}")
        val corners = assertNotNull(frame)
        listOf(0.3f to 0.2f, 0.7f to 0.2f, 0.7f to 0.8f, 0.3f to 0.8f).zip(corners).forEach { (expected, found) ->
            assertTrue(abs(expected.first - found.first) < 0.03f && abs(expected.second - found.second) < 0.03f, "corner $found")
        }
    }

    @Test fun anEmptyPhotoAsksForARetakeWithoutACard() {
        val dark = PackImage(200, 150, IntArray(200 * 150) { GROUND })
        var framed = false
        val result = PackPipeline(FakeImaging(dark), SubjectIsBright()).scan(byteArrayOf(1), {}, { framed = true })
        assertNull(result.card)
        assertEquals(PackNotFound, result.moderation)
        assertFalse(framed)
    }

    @Test fun aMissingSegmenterFailsTheScan() {
        val networks = object : PackNetworks {
            override fun segment(input: FloatArray): FloatArray = throw BagScanFailure(BagScanIssue.ModelMissing)
            override fun inpaint(image: ByteArray, known: ByteArray, width: Int, height: Int) = image
        }
        val failure = assertFailsWith<BagScanFailure> { PackPipeline(FakeImaging(bagPhoto()), networks).scan(byteArrayOf(1), {}, {}) }
        assertEquals(BagScanIssue.ModelMissing, failure.issue)
    }

    @Test fun inpaintingSendsChannelsAndKnownMaskAndKeepsAlpha() {
        var sent: Pair<ByteArray, ByteArray>? = null
        val networks = object : PackNetworks {
            override fun segment(input: FloatArray) = input
            override fun inpaint(image: ByteArray, known: ByteArray, width: Int, height: Int): ByteArray {
                sent = image to known
                return ByteArray(image.size) { 0x10 }
            }
        }
        val image = PackImage(2, 1, intArrayOf(0xFF102030.toInt(), 0x80405060.toInt()))
        val result = networks.inpainted(image, booleanArrayOf(false, true))
        val (channels, known) = assertNotNull(sent)
        assertEquals(listOf(0x10, 0x40, 0x20, 0x50, 0x30, 0x60), channels.map { it.toInt() and 0xFF })
        assertEquals(listOf(255, 0), known.map { it.toInt() and 0xFF })
        assertEquals(listOf(0xFF101010.toInt(), 0x80101010.toInt()), result.pixels.toList())
    }
}

private const val GROUND = 0xFF202020.toInt()
private const val BAG = 0xFFE0C080.toInt()

/** A 400 x 300 dark table with a bright 160 x 180 bag in the middle. */
private fun bagPhoto(): PackImage {
    val width = 400
    val height = 300
    return PackImage(width, height, IntArray(width * height) {
        val x = it % width
        val y = it / width
        if (x in 120 until 280 && y in 60 until 240) BAG else GROUND
    })
}

/** Decodes every photo to [photo]; the rest as the platforms do it, but in plain Kotlin. */
private class FakeImaging(private val photo: PackImage) : PackImaging {
    override fun decode(photo: ByteArray, maxSide: Int) = this.photo

    override fun scaledDown(image: PackImage, maxSide: Int) = image

    override fun rectified(image: PackImage, quad: Quad, width: Int, height: Int) =
        PackImage(width, height, rectify(image.pixels, image.width, image.height, quad, width, height))

    override fun jpeg(image: PackImage, quality: Int) = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
}

/** A segmenter that sees bright pixels as the subject: the red channel of its normalised input. */
private class SubjectIsBright : PackNetworks {
    override fun segment(input: FloatArray): FloatArray = input.copyOf(SEGMENTER_SIDE * SEGMENTER_SIDE)

    override fun inpaint(image: ByteArray, known: ByteArray, width: Int, height: Int) = image
}
