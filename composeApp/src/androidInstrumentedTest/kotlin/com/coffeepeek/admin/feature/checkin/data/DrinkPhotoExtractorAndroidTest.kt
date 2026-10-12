package com.coffeepeek.admin.feature.checkin.data

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coffeepeek.composeapp.generated.resources.Res
import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DrinkPhotoExtractorAndroidTest {
    @Test
    fun realSegmenterProducesTransparentPngAndPreservesOriginal() = runBlocking {
        val original = Res.readBytes("drawable/drink_mock_cappuccino.png")
        val snapshot = original.copyOf()
        val extractor = createDrinkPhotoExtractor()
        val input = PendingPhotoUpload("cup.png", "image/png", original)
        var failure: Exception? = null
        // The unbundled model may still be downloading on a fresh emulator.
        val cutouts = withTimeout(120_000) {
            var result = emptyList<PendingPhotoUpload>()
            while (result.isEmpty()) {
                try {
                    result = extractor.extract(input)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    failure = error
                }
                if (result.isEmpty()) delay(2_000)
            }
            result
        }
        assertTrue(failure?.message, cutouts.isNotEmpty())
        assertArrayEquals(snapshot, original)
        val output = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "drink-photo-test")
        assertTrue(output.isDirectory || output.mkdirs())
        File(output, "original.png").writeBytes(original)
        cutouts.forEachIndexed { index, photo ->
            assertEquals("image/png", photo.contentType)
            assertTrue(photo.fileName.endsWith(".png"))
            assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), photo.bytes.take(8).toByteArray())
            val bitmap = requireNotNull(BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.size))
            try {
                assertTrue(bitmap.width in 1..768 && bitmap.height in 1..768)
                assertTrue(bitmap.hasAlpha())
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                assertTrue("Cutout must have transparent background", pixels.any { Color.alpha(it) == 0 })
                assertTrue("Cutout must retain the subject", pixels.any { Color.alpha(it) > 200 })
                File(output, "cutout-$index.png").writeBytes(photo.bytes)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test
    fun invalidImageFailsWithoutCrashingTheApp() = runBlocking {
        try {
            createDrinkPhotoExtractor().extract(PendingPhotoUpload("broken.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
            fail("Invalid image must be rejected")
        } catch (error: IllegalStateException) {
            assertEquals("Failed to decode image", error.message)
        }
    }
}
