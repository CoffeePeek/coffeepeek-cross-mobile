package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.ImageIO.*
import platform.posix.memcpy
import kotlin.math.abs
import kotlin.test.*

@OptIn(ExperimentalForeignApi::class)
class IosPackImagingTest {
    /** 40 x 20: red on the left half, blue on the right. */
    private val wide = PackImage(40, 20, IntArray(40 * 20) { if (it % 40 < 20) RED else BLUE })

    @Test fun aJpegCardReadsBackAtItsSizeAndColours() {
        val photo = IosPackImaging.decode(IosPackImaging.jpeg(wide, 95), maxSide = 2400)
        assertEquals(40 to 20, photo.width to photo.height)
        assertClose(RED, photo.pixels[10 * 40 + 5])
        assertClose(BLUE, photo.pixels[10 * 40 + 35])
    }

    @Test fun aLargePhotoIsDecodedDownToTheLimit() {
        val photo = IosPackImaging.decode(IosPackImaging.jpeg(wide, 95), maxSide = 10)
        assertEquals(10 to 5, photo.width to photo.height)
    }

    @Test fun exifOrientationIsAppliedSoThePhotoIsUpright() {
        // Orientation 6: the stored pixels must be turned 90° clockwise to be upright, as an iPhone held in portrait stores them.
        val photo = IosPackImaging.decode(jpegWithOrientation(wide, orientation = 6), maxSide = 2400)
        assertEquals(20 to 40, photo.width to photo.height)
        assertClose(RED, photo.pixels[5 * 20 + 10])
        assertClose(BLUE, photo.pixels[35 * 20 + 10])
    }

    @Test fun scalingDownKeepsTheProportionsAndNeverScalesUp() {
        assertEquals(20 to 10, IosPackImaging.scaledDown(wide, 20).let { it.width to it.height })
        assertSame(wide, IosPackImaging.scaledDown(wide, 100))
    }

    @Test fun bytesThatAreNoImageAreAnUnreadablePhoto() {
        for (photo in listOf(ByteArray(0), "not a photo".encodeToByteArray())) {
            assertEquals(BagScanIssue.UnreadablePhoto, assertFailsWith<BagScanFailure> { IosPackImaging.decode(photo, 2400) }.issue)
        }
    }
}

private const val RED = 0xFFD02010.toInt()
private const val BLUE = 0xFF1030C0.toInt()

private fun assertClose(expected: Int, actual: Int) {
    for (shift in intArrayOf(16, 8, 0)) {
        val difference = abs((expected shr shift and 0xFF) - (actual shr shift and 0xFF))
        assertTrue(difference <= 12, "expected ${expected.toUInt().toString(16)}, got ${actual.toUInt().toString(16)}")
    }
}

/** [image] as a JPEG whose EXIF says it must be shown with [orientation]. */
@OptIn(ExperimentalForeignApi::class)
private fun jpegWithOrientation(image: PackImage, orientation: Int): ByteArray = memScoped {
    val space = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
    val format = CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value or kCGBitmapByteOrder32Little
    val context = CGBitmapContextCreate(null, image.width.toULong(), image.height.toULong(), 8u, (4 * image.width).toULong(), space, format)
    image.pixels.usePinned { memcpy(CGBitmapContextGetData(context), it.addressOf(0), (4 * image.pixels.size).toULong()) }
    val cgImage = CGBitmapContextCreateImage(context)
    val output = CFDataCreateMutable(null, 0)
    val type = CFStringCreateWithCString(null, "public.jpeg", kCFStringEncodingUTF8)
    val destination = CGImageDestinationCreateWithData(output, type, 1u, null)
    val properties = CFDictionaryCreateMutable(null, 1, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
    val value = alloc<IntVar>().apply { this.value = orientation }
    val number = CFNumberCreate(null, kCFNumberIntType, value.ptr)
    CFDictionarySetValue(properties, kCGImagePropertyOrientation, number)
    CGImageDestinationAddImage(destination, cgImage, properties)
    check(CGImageDestinationFinalize(destination))
    val bytes = CFDataGetBytePtr(output)!!.readBytes(CFDataGetLength(output).toInt())
    listOf(number, properties, destination, type, output).forEach { CFRelease(it) }
    CGImageRelease(cgImage)
    CGContextRelease(context)
    CGColorSpaceRelease(space)
    bytes
}
