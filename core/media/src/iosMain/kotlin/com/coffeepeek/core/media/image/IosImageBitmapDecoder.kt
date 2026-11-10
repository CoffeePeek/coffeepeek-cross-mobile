@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.media.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import io.kamel.core.config.ResourceConfig
import io.kamel.core.decoder.Decoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFNumberLongLongType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceShouldCache
import platform.ImageIO.kCGImageSourceShouldCacheImmediately
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import kotlin.reflect.KClass

/** Downsamples images before allocating the Compose bitmap. */
internal class IosImageBitmapDecoder(
    private val maxDimension: Int = 2048,
) : Decoder<ImageBitmap> {
    private val decodeMutex = Mutex()

    init {
        require(maxDimension > 0 && maxDimension.toLong() * maxDimension <= Int.MAX_VALUE / 4)
    }

    override val outputKClass: KClass<ImageBitmap> = ImageBitmap::class

    override suspend fun decode(
        channel: ByteReadChannel,
        resourceConfig: ResourceConfig,
    ): ImageBitmap = withContext(Dispatchers.Default) {
        // Kamel caches by URL, without the requested decode size. Use one bounded size for
        // both the feed and viewer so a thumbnail cannot poison the viewer's cache entry.
        // Serializing the read as well as decode also bounds simultaneous temporary buffers.
        decodeMutex.withLock {
            currentCoroutineContext().ensureActive()
            val bytes = channel.toByteArray()
            currentCoroutineContext().ensureActive()
            require(bytes.isNotEmpty()) { "Image data is empty" }
            val bitmap = decodeBitmap(bytes)
            try {
                currentCoroutineContext().ensureActive()
                bitmap.asComposeImageBitmap()
            } catch (failure: Throwable) {
                bitmap.close()
                throw failure
            }
        }
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap = memScoped {
        val data = checkNotNull(bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        }) { "Unable to create image data" }
        data.useCf {
            val sourceOptions = checkNotNull(CFDictionaryCreateMutable(
                null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr,
            ))
            sourceOptions.useCf {
                CFDictionarySetValue(sourceOptions, kCGImageSourceShouldCache, kCFBooleanFalse)
                val source = requireNotNull(CGImageSourceCreateWithData(data, sourceOptions)) {
                    "Unsupported or invalid image data"
                }
                source.useCf {
                    val properties = requireNotNull(CGImageSourceCopyPropertiesAtIndex(source, 0u, sourceOptions)) {
                        "Image dimensions are unavailable"
                    }
                    val sourceMaxDimension = properties.useCf {
                        maxOf(
                            imageDimension(properties, kCGImagePropertyPixelWidth),
                            imageDimension(properties, kCGImagePropertyPixelHeight),
                        )
                    }
                    // ImageIO can scale thumbnails up. Clamp to source dimensions explicitly.
                    val pixelLimit = alloc<IntVar> {
                        value = minOf(maxDimension.toLong(), sourceMaxDimension).toInt()
                    }
                    val pixelLimitNumber = checkNotNull(CFNumberCreate(null, kCFNumberIntType, pixelLimit.ptr))
                    pixelLimitNumber.useCf {
                        val thumbnailOptions = checkNotNull(CFDictionaryCreateMutable(
                            null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr,
                        ))
                        thumbnailOptions.useCf {
                            CFDictionarySetValue(thumbnailOptions, kCGImageSourceThumbnailMaxPixelSize, pixelLimitNumber)
                            CFDictionarySetValue(thumbnailOptions, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
                            CFDictionarySetValue(thumbnailOptions, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
                            CFDictionarySetValue(thumbnailOptions, kCGImageSourceShouldCacheImmediately, kCFBooleanTrue)
                            val thumbnail = requireNotNull(CGImageSourceCreateThumbnailAtIndex(source, 0u, thumbnailOptions)) {
                                "Unable to decode bounded image"
                            }
                            try {
                                copyThumbnail(thumbnail)
                            } finally {
                                CGImageRelease(thumbnail)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun imageDimension(properties: CFDictionaryRef, key: CFStringRef?): Long = memScoped {
        val number: CFNumberRef = requireNotNull(CFDictionaryGetValue(properties, key)).reinterpret()
        val dimension = alloc<LongVar>()
        require(CFNumberGetValue(number, kCFNumberLongLongType, dimension.ptr) && dimension.value > 0) {
            "Invalid image dimensions"
        }
        dimension.value
    }

    private fun copyThumbnail(image: CGImageRef): Bitmap {
        val width = CGImageGetWidth(image).toInt()
        val height = CGImageGetHeight(image).toInt()
        require(width in 1..maxDimension && height in 1..maxDimension) { "Decoded image exceeds its pixel limit" }
        val rowBytes = width * 4
        val pixels = ByteArray(rowBytes * height)
        val colorSpace = checkNotNull(CGColorSpaceCreateWithName(kCGColorSpaceSRGB))
        try {
            pixels.usePinned { pinned ->
                val context = checkNotNull(CGBitmapContextCreate(
                    pinned.addressOf(0), width.toULong(), height.toULong(), 8u, rowBytes.toULong(),
                    colorSpace,
                    kCGBitmapByteOrder32Big or CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
                )) { "Unable to allocate bounded bitmap context" }
                try {
                    // An untransformed bitmap context writes CGImage scanlines in the same
                    // order as Skia's raster buffer. ImageIO has already applied EXIF rotation.
                    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), image)
                } finally {
                    CGContextRelease(context)
                }
            }
        } finally {
            CGColorSpaceRelease(colorSpace)
        }
        val bitmap = Bitmap()
        try {
            // Skiko 0.150.1 installPixels mallocs and copies this byte array; its PixelRef
            // frees that copy. The Compose wrapper then shares the bitmap without copying.
            check(bitmap.installPixels(
                ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL, ColorSpace.sRGB),
                pixels, rowBytes,
            )) { "Unable to create bounded image bitmap" }
            bitmap.setImmutable()
            return bitmap
        } catch (failure: Throwable) {
            bitmap.close()
            throw failure
        }
    }
}

private inline fun <T : CPointed, R> CPointer<T>.useCf(block: () -> R): R = try {
    block()
} finally {
    CFRelease(this)
}
