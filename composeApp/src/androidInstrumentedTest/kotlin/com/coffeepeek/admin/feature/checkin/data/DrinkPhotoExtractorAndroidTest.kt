package com.coffeepeek.admin.feature.checkin.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class DrinkPhotoExtractorAndroidTest {
    @Test
    fun bundledModelsProduceTransparentCupPngWithoutDownloadingAndPreserveOriginal() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val original = instrumentation.context.assets.open("checkin/coffee.jpg").use { it.readBytes() }
        val snapshot = original.copyOf()
        val extractor = createDrinkPhotoExtractor()
        val input = PendingPhotoUpload("coffee.jpg", "image/jpeg", original)
        val cutouts = withTimeout(60_000) { extractor.extract(input) }
        assertEquals("Find the cup rather than offering the whole foreground", 1, cutouts.size)
        assertArrayEquals(snapshot, original)
        cutouts.forEach(::assertValidCutout)
        val cup = requireNotNull(BitmapFactory.decodeByteArray(cutouts.single().bytes, 0, cutouts.single().bytes.size))
        try {
            val pixels = IntArray(cup.width * cup.height)
            cup.getPixels(pixels, 0, cup.width, 0, 0, cup.width, cup.height)
            val porcelainPixels = pixels.count {
                Color.alpha(it) > 240 && Color.red(it) > 190 && Color.green(it) > 190 &&
                    Color.blue(it) > 170 && kotlin.math.abs(Color.red(it) - Color.blue(it)) < 45
            }
            // A transparent crop of the wooden background is not a successful cup extraction.
            assertTrue("The cutout must contain the fixture's pale ceramic cup", porcelainPixels > 100)
            assertTrue("The whole cup must be retained", cup.width >= 180 && cup.height >= 180)
        } finally { cup.recycle() }
    }

    @Test
    fun noCupIsOfferedForAnEmptyBackground() = runBlocking {
        val image = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val bytes = try { image.eraseColor(Color.WHITE); image.pngBytes() } finally { image.recycle() }
        assertTrue(createDrinkPhotoExtractor().extract(PendingPhotoUpload("blank.png", "image/png", bytes)).isEmpty())
    }

    @Test
    fun objectMaskRemovesHandPixelsKeepsCupColorAndCropsWithTransparentPadding() {
        val image = Bitmap.createBitmap(12, 10, Bitmap.Config.ARGB_8888)
        image.eraseColor(Color.rgb(210, 160, 150)) // Hand and a pink cup may have the same colour.
        val original = image.pngBytes()
        val mask = FloatArray(120)
        for (y in 2..7) for (x in 4..8) mask[y * 12 + x] = 1f
        mask[2 * 12 + 4] = 0.5f
        mask[0] = Float.NaN
        val output = requireNotNull(maskDrinkSubject(image, mask))
        try {
            assertEquals(9, output.width)
            assertEquals(10, output.height)
            assertEquals(0, Color.alpha(output.getPixel(0, 0)))
            assertEquals(Color.rgb(210, 160, 150), output.getPixel(4, 4))
            assertTrue(Color.alpha(output.getPixel(2, 2)) in 126..129)
            assertArrayEquals(original, image.pngBytes())
            assertValidCutout(requireNotNull(encodeDrinkSubject(output, 0)))
        } finally { output.recycle(); image.recycle() }
    }

    @Test
    fun emptyObjectMaskDoesNotCreateAnInvalidPng() {
        val image = Bitmap.createBitmap(12, 10, Bitmap.Config.ARGB_8888)
        try { assertNull(maskDrinkSubject(image, FloatArray(120))) } finally { image.recycle() }
    }

    /** Optional private regression photo, passed at run time rather than committed to Git. */
    @Test
    fun suppliedCupInHandKeepsTheCupAndRemovesTheHand() = runBlocking {
        val path = InstrumentationRegistry.getArguments().getString("cupInHandFixture")
        org.junit.Assume.assumeTrue("Pass -e cupInHandFixture <device path> to check the private photo", path != null)
        val source = File(requireNotNull(path)).readBytes()
        val photo = createDrinkPhotoExtractor().extract(PendingPhotoUpload("cup-in-hand.png", "image/png", source)).single()
        assertValidCutout(photo)
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "cup-result")
        folder.mkdirs()
        File(folder, "cup-without-hand.png").writeBytes(photo.bytes)
        val output = requireNotNull(BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.size))
        try {
            // The visible cup is almost square; retaining the arm would make a tall cutout.
            assertTrue("Hand/arm must not extend the cup bounds", output.height.toFloat() / output.width in 0.8f..1.4f)
            val x = (output.width * 0.58f).toInt()
            val y = (output.height * 0.94f).toInt()
            assertEquals("Thumb below the printed cup must be transparent", 0, Color.alpha(output.getPixel(x, y)))
            assertEquals("Cup body must remain visible", 255, Color.alpha(output.getPixel(output.width / 2, output.height / 2)))
        } finally { output.recycle() }
    }

    @Test
    fun resizingAndEncodingRetainTransparencyAndDoNotMutateTheSubject() {
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        try {
            Canvas(bitmap).drawCircle(600f, 400f, 250f, Paint().apply { color = Color.rgb(90, 50, 20) })
            val snapshot = bitmap.pngBytes()
            val cutout = requireNotNull(encodeDrinkSubject(bitmap, 0))

            assertValidCutout(cutout)
            val output = requireNotNull(BitmapFactory.decodeByteArray(cutout.bytes, 0, cutout.bytes.size))
            try { assertEquals(768, output.width) } finally { output.recycle() }
            assertFalse(bitmap.isRecycled)
            assertArrayEquals(snapshot, bitmap.pngBytes())
        } finally { bitmap.recycle() }
    }

    @Test
    fun emptyAndOpaqueSubjectsAreNotOfferedAsInvalidCutouts() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            assertNull(encodeDrinkSubject(bitmap, 0))
            bitmap.eraseColor(Color.BLUE)
            assertNull(encodeDrinkSubject(bitmap, 0))
        } finally { bitmap.recycle() }
    }

    @Test
    fun semitransparentSubjectWithoutFullyClearPixelsMatchesTheUploadContract() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.argb(128, 90, 50, 20))
            val cutout = requireNotNull(encodeDrinkSubject(bitmap, 0))
            assertValidCutout(cutout)
            val output = requireNotNull(BitmapFactory.decodeByteArray(cutout.bytes, 0, cutout.bytes.size))
            try { assertEquals(128, Color.alpha(output.getPixel(16, 16))) } finally { output.recycle() }
        } finally { bitmap.recycle() }
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

    private fun assertValidCutout(photo: PendingPhotoUpload) {
        assertEquals("image/png", photo.contentType)
        assertTrue(photo.fileName.endsWith(".png"))
        assertTrue(photo.bytes.size in 1..4_194_304)
        assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), photo.bytes.take(8).toByteArray())
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.size))
        try {
            assertTrue(bitmap.width in 1..768 && bitmap.height in 1..768)
            assertTrue(bitmap.hasAlpha())
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("Cutout must contain transparency", pixels.any { Color.alpha(it) < 255 })
            assertTrue("Cutout must retain visible pixels", pixels.any { Color.alpha(it) > 0 })
        } finally { bitmap.recycle() }
    }

    private fun Bitmap.pngBytes(): ByteArray = ByteArrayOutputStream().use { output ->
        check(compress(Bitmap.CompressFormat.PNG, 100, output))
        output.toByteArray()
    }
}
