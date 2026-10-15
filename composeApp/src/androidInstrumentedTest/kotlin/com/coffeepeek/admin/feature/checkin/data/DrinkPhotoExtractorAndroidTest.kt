package com.coffeepeek.admin.feature.checkin.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coffeepeek.composeapp.generated.resources.Res
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RunWith(AndroidJUnit4::class)
class DrinkPhotoExtractorAndroidTest {
    @Test
    fun realSegmenterProducesTransparentPngAndPreservesOriginal() = runBlocking {
        // Reuse the existing coffee mascot on an opaque background: no large photo fixture in the APK.
        val asset = Res.readBytes("drawable/checkin_rating_coffee.png")
        val subject = requireNotNull(BitmapFactory.decodeByteArray(asset, 0, asset.size))
        val image = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val original = try {
            Canvas(image).apply {
                drawColor(Color.WHITE)
                drawBitmap(subject, (width - subject.width) / 2f, (height - subject.height) / 2f, null)
            }
            image.pngBytes()
        } finally {
            subject.recycle()
            image.recycle()
        }
        val snapshot = original.copyOf()
        ensureSubjectModelAvailable()
        val extractor = createDrinkPhotoExtractor()
        val input = PendingPhotoUpload("coffee.png", "image/png", original)
        val cutouts = withTimeout(60_000) { extractor.extract(input) }
        assertTrue("Installed ML Kit model produced no foreground candidates for the fixture", cutouts.isNotEmpty())
        assertArrayEquals(snapshot, original)
        cutouts.forEach(::assertValidCutout)
    }

    /** Sideloaded test APKs must not rely on Play Store's deferred, install-time model download. */
    private suspend fun ensureSubjectModelAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modules = ModuleInstall.getClient(context)
        val subjectApi = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().build())
        val lastStatus = AtomicReference("Checking model availability")
        val failure = AtomicReference<String?>(null)
        val listener = InstallStatusListener { update ->
            val message = "state=${update.installState}, error=${update.errorCode}, " +
                "downloaded=${update.progressInfo?.bytesDownloaded}/${update.progressInfo?.totalBytesToDownload}"
            lastStatus.set(message)
            Log.i("DrinkPhotoModelTest", message)
            if (update.installState == InstallState.STATE_FAILED || update.installState == InstallState.STATE_CANCELED) {
                failure.set(message)
            }
        }
        try {
            withTimeout(180_000) {
                if (modules.areModulesAvailable(subjectApi).awaitTestTask().areModulesAvailable()) return@withTimeout
                val response = modules.installModules(ModuleInstallRequest.newBuilder()
                    .addApi(subjectApi).setListener(listener).build()).awaitTestTask()
                Log.i("DrinkPhotoModelTest", "Install requested: session=${response.sessionId}, " +
                    "alreadyInstalled=${response.areModulesAlreadyInstalled()}")
                while (!modules.areModulesAvailable(subjectApi).awaitTestTask().areModulesAvailable()) {
                    failure.get()?.let { error("ML Kit model installation failed: $it") }
                    delay(1_000)
                }
                Log.i("DrinkPhotoModelTest", "Subject segmentation model is available")
            }
        } catch (timeout: TimeoutCancellationException) {
            throw AssertionError("ML Kit model installation timed out: ${lastStatus.get()}", timeout)
        } finally {
            modules.unregisterListener(listener)
            subjectApi.close()
        }
    }

    private suspend fun <T> Task<T>.awaitTestTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (continuation.isActive) {
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(task.exception ?: IllegalStateException("Google Play services task failed"))
            }
        }
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
