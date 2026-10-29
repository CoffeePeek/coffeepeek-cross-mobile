package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.data.scan.imaging.Quad
import com.coffeepeek.feature.shelf.data.scan.imaging.rectify
import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.ImageIO.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The scan's image work on ImageIO and Core Graphics. Photos are read upright (EXIF orientation applied, HEIC included) and in
 * sRGB, as the models expect; the perspective stretch is the shared Kotlin one.
 */
@OptIn(ExperimentalForeignApi::class)
internal object IosPackImaging : PackImaging {
    override fun decode(photo: ByteArray, maxSide: Int): PackImage {
        val image = thumbnail(photo, maxSide) ?: throw BagScanFailure(BagScanIssue.UnreadablePhoto)
        try {
            val width = CGImageGetWidth(image).toInt()
            val height = CGImageGetHeight(image).toInt()
            return PackImage(width, height, draw(width, height) { CGContextDrawImage(it, rect(width, height), image) })
        } finally {
            CGImageRelease(image)
        }
    }

    override fun scaledDown(image: PackImage, maxSide: Int): PackImage {
        val factor = maxSide.toDouble() / max(image.width, image.height)
        if (factor >= 1.0) return image
        val width = (image.width * factor).roundToInt()
        val height = (image.height * factor).roundToInt()
        return image.withCgImage { source ->
            PackImage(width, height, draw(width, height) {
                CGContextSetInterpolationQuality(it, kCGInterpolationHigh)
                CGContextDrawImage(it, rect(width, height), source)
            })
        }
    }

    override fun rectified(image: PackImage, quad: Quad, width: Int, height: Int): PackImage =
        PackImage(width, height, rectify(image.pixels, image.width, image.height, quad, width, height))

    override fun jpeg(image: PackImage, quality: Int): ByteArray = image.withCgImage { source ->
        val data = UIImageJPEGRepresentation(UIImage.imageWithCGImage(source), quality / 100.0) ?: error("JPEG encoding failed")
        data.toByteArray()
    }
}

/** The photo decoded at most [maxSide] on its longer side straight from the file, without the full-size bitmap. */
@OptIn(ExperimentalForeignApi::class)
private fun thumbnail(photo: ByteArray, maxSide: Int): CGImageRef? {
    if (photo.isEmpty()) return null
    val data = photo.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), photo.size.toLong()) } ?: return null
    val options = thumbnailOptions(maxSide)
    try {
        val source = CGImageSourceCreateWithData(data, null) ?: return null
        return CGImageSourceCreateThumbnailAtIndex(source, 0u, options).also { CFRelease(source) }
    } finally {
        CFRelease(options)
        CFRelease(data)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun thumbnailOptions(maxSide: Int): CFDictionaryRef? = memScoped {
    val options = CFDictionaryCreateMutable(null, 3, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
    val side = alloc<IntVar>().apply { value = maxSide }
    val sideNumber = CFNumberCreate(null, kCFNumberIntType, side.ptr)
    CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
    CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
    CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, sideNumber)
    CFRelease(sideNumber)
    options
}

/** ARGB ints are 0xAARRGGBB, which is BGRA in little-endian memory. */
@OptIn(ExperimentalForeignApi::class)
private val PIXEL_FORMAT = CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value or kCGBitmapByteOrder32Little

/** Pixels of a [width] x [height] sRGB bitmap after [paint]; the bitmap memory is Core Graphics' own while it paints. */
@OptIn(ExperimentalForeignApi::class)
private fun draw(width: Int, height: Int, paint: (CGContextRef?) -> Unit): IntArray = bitmap(width, height) { context ->
    paint(context)
    IntArray(width * height).also { pixels ->
        pixels.usePinned { memcpy(it.addressOf(0), CGBitmapContextGetData(context), (4 * pixels.size).toULong()) }
    }
}

/** [image] as a Core Graphics image for the duration of [use]. */
@OptIn(ExperimentalForeignApi::class)
private fun <T> PackImage.withCgImage(use: (CGImageRef?) -> T): T {
    val image = bitmap(width, height) { context ->
        pixels.usePinned { memcpy(CGBitmapContextGetData(context), it.addressOf(0), (4 * pixels.size).toULong()) }
        CGBitmapContextCreateImage(context)
    } ?: error("Core Graphics could not create an image")
    try {
        return use(image)
    } finally {
        CGImageRelease(image)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun <T> bitmap(width: Int, height: Int, use: (CGContextRef?) -> T): T {
    val space = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
    val context = CGBitmapContextCreate(null, width.toULong(), height.toULong(), 8u, (4 * width).toULong(), space, PIXEL_FORMAT)
    try {
        checkNotNull(context) { "Core Graphics could not create a ${width}x$height bitmap" }
        return use(context)
    } finally {
        CGContextRelease(context)
        CGColorSpaceRelease(space)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun rect(width: Int, height: Int) = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) } }
}
