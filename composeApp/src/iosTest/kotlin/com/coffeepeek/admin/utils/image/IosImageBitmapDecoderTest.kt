package com.coffeepeek.admin.utils.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.coffeepeek.admin.di.platformImageConfig
import com.coffeepeek.admin.utils.CustomUrl
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.ResourceConfig
import io.kamel.core.config.ResourceConfigBuilder
import io.kamel.core.config.takeFrom
import io.kamel.core.fetcher.Fetcher
import io.kamel.core.loadImageBitmapResource
import io.kamel.image.config.Default
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.reflect.KClass

class IosImageBitmapDecoderTest {
    @Test
    fun invalidDimensionLimitsAreRejectedWithoutArithmeticOverflow() {
        for (limit in listOf(0, -1, Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { IosImageBitmapDecoder(limit) }
        }
    }

    @Test
    fun largeLandscapeAndPortraitAreDownsampledBeforeBecomingComposeBitmaps() = runTest {
        withDecoded(encodeFixture(4096, 32)) { image ->
            assertEquals(2048, image.width)
            assertEquals(16, image.height)
        }
        withDecoded(encodeFixture(32, 4096)) { image ->
            assertEquals(16, image.width)
            assertEquals(2048, image.height)
        }
    }

    @Test
    fun smallImageIsNotUpscaled() = runTest {
        withDecoded(encodeFixture(24, 12)) { image ->
            assertEquals(24, image.width)
            assertEquals(12, image.height)
        }
    }

    @Test
    fun asymmetricPngKeepsTopBottomOrderAndAlpha() = runTest {
        val png = encodeFixture(32, 24) { x, y ->
            when {
                x < 16 && y < 12 -> RED
                x >= 16 && y < 12 -> 0x8000ff00.toInt()
                x < 16 -> BLUE
                else -> 0
            }
        }
        withDecoded(png) { image ->
            assertPixel(image, 8, 6, RED)
            assertPixel(image, 24, 6, 0x8000ff00.toInt(), tolerance = 2)
            assertPixel(image, 8, 18, BLUE)
            assertEquals(0, pixel(image, 24, 18).ushr(24))
        }
    }

    @Test
    fun jpegExifClockwiseRotationIsApplied() = runTest {
        val jpeg = encodeFixture(96, 64, EncodedImageFormat.JPEG)
        withDecoded(withExifOrientation(jpeg, 6)) { image ->
            assertEquals(64, image.width)
            assertEquals(96, image.height)
            assertPixel(image, 16, 24, BLUE, tolerance = 12)
            assertPixel(image, 48, 24, RED, tolerance = 12)
            assertPixel(image, 16, 72, YELLOW, tolerance = 12)
            assertPixel(image, 48, 72, GREEN, tolerance = 12)
        }
    }

    @Test
    fun jpegExifHorizontalMirroringIsApplied() = runTest {
        val jpeg = encodeFixture(96, 64, EncodedImageFormat.JPEG)
        withDecoded(withExifOrientation(jpeg, 2)) { image ->
            assertEquals(96, image.width)
            assertEquals(64, image.height)
            assertPixel(image, 24, 16, GREEN, tolerance = 12)
            assertPixel(image, 72, 16, RED, tolerance = 12)
            assertPixel(image, 24, 48, YELLOW, tolerance = 12)
            assertPixel(image, 72, 48, BLUE, tolerance = 12)
        }
    }

    @Test
    fun invalidDataFailsWithoutAnOriginalSizeFallback() = runTest {
        val decoder = IosImageBitmapDecoder()
        assertFailsWith<IllegalArgumentException> { decode(decoder, byteArrayOf()) }
        assertFailsWith<IllegalArgumentException> { decode(decoder, "not an image".encodeToByteArray()) }
    }

    @Test
    fun applicationImageConfigUsesBoundedDecoderForViewerAndFeedSources() = runTest {
        val bytes = encodeFixture(4096, 32)
        val config = platformImageConfig(KamelConfig {
            takeFrom(KamelConfig.Default)
            fetcher(fixtureFetcher(Url::class, bytes))
            fetcher(fixtureFetcher(CustomUrl::class, bytes))
        })
        assertIs<ImageBitmapMemoryCache>(config.imageBitmapCache)
        val images = mutableListOf<ImageBitmap>()
        try {
            // The viewer supplies a String, which Kamel maps to Url; CpImage supplies CustomUrl.
            for (source in listOf("https://example.invalid/viewer.jpg", CustomUrl("https://example.invalid/feed.jpg"))) {
                val result = config.loadImageBitmapResource(
                    source, ResourceConfigBuilder(EmptyCoroutineContext).build(),
                ).single()
                val image = assertIs<Resource.Success<ImageBitmap>>(result).value
                images += image
                assertEquals(2048, image.width)
                assertEquals(16, image.height)
            }
            assertEquals(2, config.imageBitmapCache.size)
        } finally {
            config.imageBitmapCache.clear()
            images.forEach { it.asSkiaBitmap().close() }
        }
    }

    private suspend fun withDecoded(bytes: ByteArray, block: (ImageBitmap) -> Unit) {
        val image = decode(IosImageBitmapDecoder(), bytes)
        try {
            block(image)
        } finally {
            image.asSkiaBitmap().close()
        }
    }

    private suspend fun decode(decoder: IosImageBitmapDecoder, bytes: ByteArray): ImageBitmap = decoder.decode(
        ByteReadChannel(bytes), ResourceConfigBuilder(EmptyCoroutineContext).build(),
    )

    private fun <T : Any> fixtureFetcher(type: KClass<T>, bytes: ByteArray): Fetcher<T> = object : Fetcher<T> {
        override val inputDataKClass: KClass<T> = type
        override val source: DataSource = DataSource.Disk
        override val T.isSupported: Boolean get() = true
        override fun fetch(data: T, resourceConfig: ResourceConfig): Flow<Resource<ByteReadChannel>> =
            flowOf(Resource.Success(ByteReadChannel(bytes), source))
    }

    private fun encodeFixture(
        width: Int,
        height: Int,
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        color: (Int, Int) -> Int = { x, y ->
            when {
                x < width / 2 && y < height / 2 -> RED
                x >= width / 2 && y < height / 2 -> GREEN
                x < width / 2 -> BLUE
                else -> YELLOW
            }
        },
    ): ByteArray {
        val rgba = ByteArray(width * height * 4)
        for (y in 0 until height) for (x in 0 until width) {
            val argb = color(x, y)
            val index = (y * width + x) * 4
            rgba[index] = (argb ushr 16).toByte()
            rgba[index + 1] = (argb ushr 8).toByte()
            rgba[index + 2] = argb.toByte()
            rgba[index + 3] = (argb ushr 24).toByte()
        }
        val bitmap = Bitmap()
        try {
            check(bitmap.installPixels(
                ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB),
                rgba, width * 4,
            ))
            bitmap.setImmutable()
            val image = Image.makeFromBitmap(bitmap)
            try {
                val data = checkNotNull(image.encodeToData(format, quality = 100))
                try {
                    return data.bytes
                } finally {
                    data.close()
                }
            } finally {
                image.close()
            }
        } finally {
            bitmap.close()
        }
    }

    private fun withExifOrientation(jpeg: ByteArray, orientation: Int): ByteArray {
        // A minimal APP1 EXIF segment with one little-endian TIFF Orientation SHORT.
        val exif = byteArrayOf(
            0x45, 0x78, 0x69, 0x66, 0, 0, 0x49, 0x49, 0x2a, 0, 8, 0, 0, 0,
            1, 0, 0x12, 1, 3, 0, 1, 0, 0, 0, orientation.toByte(), 0, 0, 0, 0, 0, 0, 0,
        )
        val length = exif.size + 2
        return jpeg.copyOfRange(0, 2) + byteArrayOf(
            0xff.toByte(), 0xe1.toByte(), (length ushr 8).toByte(), length.toByte(),
        ) + exif + jpeg.copyOfRange(2, jpeg.size)
    }

    private fun assertPixel(image: ImageBitmap, x: Int, y: Int, expected: Int, tolerance: Int = 0) {
        val actual = pixel(image, x, y)
        for (shift in listOf(24, 16, 8, 0)) {
            assertTrue(abs((actual ushr shift and 255) - (expected ushr shift and 255)) <= tolerance,
                "Unexpected pixel at ($x, $y): ${actual.toUInt().toString(16)}; expected ${expected.toUInt().toString(16)}")
        }
    }

    private fun pixel(image: ImageBitmap, x: Int, y: Int): Int = IntArray(1).also {
        image.readPixels(it, startX = x, startY = y, width = 1, height = 1)
    }.single()

    private companion object {
        const val RED = 0xffff0000.toInt()
        const val GREEN = 0xff00ff00.toInt()
        const val BLUE = 0xff0000ff.toInt()
        const val YELLOW = 0xffffff00.toInt()
    }
}
