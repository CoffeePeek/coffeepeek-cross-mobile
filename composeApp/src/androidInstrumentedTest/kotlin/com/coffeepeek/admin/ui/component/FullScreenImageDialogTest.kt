package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.click
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.ResourceConfig
import io.kamel.core.decoder.Decoder
import io.kamel.core.fetcher.Fetcher
import io.kamel.image.config.LocalKamelConfig
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import com.coffeepeek.admin.utils.CustomUrl
import com.coffeepeek.admin.utils.image.PhotoPermission
import com.coffeepeek.admin.utils.image.ProtectedPhotoSession
import com.coffeepeek.admin.utils.image.SystemPhotoCacheClock
import java.util.concurrent.CopyOnWriteArrayList
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Shared viewer lifecycle checks; native iOS memory/rendering still needs device profiling. */
@RunWith(AndroidJUnit4::class)
class FullScreenImageDialogTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun openingMiddlePhotoPrefetchesOnlyAdjacentPreviewsAndSwipingAdvancesTheWindow() {
        val loader = FixtureImages(pending = false)
        val urls = List(5) { "https://viewer-fixture.invalid/photo-$it.png" }
        withViewer(loader, urls, initialIndex = 2) { _ ->
            compose.onNodeWithText(OPEN).performClick()
            val expected = urls.subList(1, 4)
            try {
                compose.waitUntil(10_000) { loader.requested.containsAll(expected) && loader.active.get() == 0 }
            } catch (timeout: ComposeTimeoutException) {
                throw AssertionError("Initial viewer window did not complete: expected=$expected; " +
                    "requested=${loader.requested.toList()}; active=${loader.active.get()}; " +
                    "cancelled=${loader.cancelled.get()}; decodedCacheEntries=${loader.config.imageBitmapCache.size}", timeout)
            }
            compose.onNodeWithText("3 / 5").assertIsDisplayed()
            compose.waitForIdle()
            assertEquals("Only current and adjacent previews may be fetched", urls.subList(1, 4).toSet(), loader.requested.toSet())

            compose.onNodeWithContentDescription("Фото 3 из 5").performTouchInput { swipeLeft() }
            compose.waitUntil(10_000) { urls[4] in loader.requested }
            compose.onNodeWithText("4 / 5").assertIsDisplayed()
            compose.onNodeWithContentDescription(CLOSE).performClick()
            compose.onNodeWithContentDescription(CLOSE).assertDoesNotExist()
            compose.onNodeWithText(PROBE).performTouchInput { click() }
        }
    }

    @Test
    fun draggingRendersBothVisiblePreviewsFromTheBoundedPrefetchWindow() {
        val loader = FixtureImages(pending = false)
        val urls = List(4) { "https://viewer-fixture.invalid/swipe-$it.png" }
        withViewer(loader, urls, initialIndex = 1) {
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { loader.requested.containsAll(urls.take(3)) && loader.active.get() == 0 }
            val current = compose.onNodeWithContentDescription("Фото 2 из 4")
            current.performTouchInput {
                down(center)
                moveTo(Offset(center.x - width * 0.3f, center.y), delayMillis = 500)
            }
            try {
                compose.waitUntil(10_000) { urls[2] in loader.requested && loader.active.get() == 0 }
                compose.waitForIdle()
                assertEquals(urls.take(3).toSet(), loader.requested.toSet())
                // Fetch completion precedes asynchronous decoding and presentation of the next frame.
                compose.waitUntil(10_000) {
                    val frame = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                        ?: return@waitUntil false
                    try {
                        isBluePreview(frame.getPixel((frame.width * 0.2f).toInt(), frame.height / 2)) &&
                            isBluePreview(frame.getPixel((frame.width * 0.85f).toInt(), frame.height / 2))
                    } finally { frame.recycle() }
                }
                val shot = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                try {
                    File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
                        "photo-viewer-partial-drag.png").outputStream().use {
                        shot.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    assertBluePreview("The outgoing visible preview must remain rendered",
                        shot.getPixel((shot.width * 0.2f).toInt(), shot.height / 2))
                    assertBluePreview("The incoming visible preview must render before currentPage flips",
                        shot.getPixel((shot.width * 0.85f).toInt(), shot.height / 2))
                } finally { shot.recycle() }
            } finally { current.performTouchInput { up() } }
        }
    }

    @Test
    fun protectedViewerLeafCarriesScopeAndRevokesItsDecodedPainterOnInvalidation() {
        val loader = FixtureImages(pending = false, pendingAfterFirst = true)
        val url = "https://viewer-fixture.invalid/api/v1/check-ins/visit/photos/scoped/variants/source/1/detail.png"
        withViewer(loader, listOf(url), images = PhotoViewerImages(listOf(url), listOf(url))) {
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { loader.requested.size == 1 && loader.active.get() == 0 }
            assertNotNull(loader.inputs.single().permissionScope)
            assertNotNull(loader.inputs.single().displayPermission)
            compose.waitForIdle()
            assertEquals(android.graphics.Color.BLUE, viewerCenterColor())

            ProtectedPhotoSession.invalidate()
            compose.waitUntil(10_000) { loader.requested.size == 2 }
            assertNotSame(loader.inputs[0].permissionScope, loader.inputs[1].permissionScope)
            compose.waitForIdle()
            assertTrue("An invalidated protected painter must disappear before a new grant",
                viewerCenterColor() != android.graphics.Color.BLUE)
        }
    }

    @Test
    fun protectedViewerLeafRemovesDecodedPainterAtItsPermissionDeadline() {
        val loader = FixtureImages(pending = false, pendingAfterFirst = true, grantMillis = 10_000)
        val url = "https://viewer-fixture.invalid/api/v1/check-ins/visit/photos/expiry/variants/source/1/detail.png"
        withViewer(loader, listOf(url), images = PhotoViewerImages(listOf(url), listOf(url))) {
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { loader.requested.size == 1 && loader.active.get() == 0 }
            compose.waitForIdle()
            assertEquals(android.graphics.Color.BLUE, viewerCenterColor())
            compose.waitUntil(20_000) { loader.requested.size == 2 }
            compose.waitForIdle()
            assertTrue("An expired protected painter must not survive pending reauthorization",
                viewerCenterColor() != android.graphics.Color.BLUE)
        }
    }

    @Test
    fun pendingHomeViewerAutomaticallyLoadsItsOriginalWithoutAnExtraAction() {
        val loader = FixtureImages(pending = false)
        val original = "https://viewer-fixture.invalid/api/v1/check-ins/home/photos/drink"
        withViewer(loader, listOf(""), images = PhotoViewerImages(listOf(""), listOf(original))) {
            compose.onNodeWithText(OPEN).performClick()
            compose.onNodeWithText("Открыть оригинал").assertDoesNotExist()
            compose.waitUntil(10_000) { loader.requested.size == 1 && loader.active.get() == 0 }
            assertEquals(listOf(original), loader.requested.toList())
            assertNotNull(loader.inputs.single().permissionScope)
            assertNotNull(loader.inputs.single().displayPermission)
        }
    }

    @Test
    fun fullScreenUsesReadyDetailVariantsWithoutTransferringTheirOriginals() {
        val loader = FixtureImages(pending = false)
        val originals = List(5) { "https://viewer-fixture.invalid/api/v1/check-ins/visit/photos/quality-$it" }
        val previews = originals.map { "$it/variants/source/1/detail.png" }
        withViewer(loader, previews, initialIndex = 2) {
            compose.onNodeWithText(OPEN).performClick()
            val openingRequests = previews.subList(1, 4).toSet()
            compose.waitUntil(10_000) { loader.requested.containsAll(openingRequests) && loader.active.get() == 0 }
            compose.onNodeWithText("Открыть оригинал").assertDoesNotExist()
            assertEquals(openingRequests, loader.requested.toSet())

            compose.onNodeWithContentDescription("Фото 3 из 5").performTouchInput { swipeLeft() }
            compose.waitUntil(10_000) { previews[4] in loader.requested }
            compose.onNodeWithText("4 / 5").assertIsDisplayed()
            assertTrue("Ready detail variants must avoid every no-store original transfer",
                loader.requested.none { it in originals })
        }
    }

    @Test
    fun readyDetailVariantIsReusedWhenTheViewerIsReopened() {
        val original = "https://viewer-fixture.invalid/api/v1/check-ins/visit/photos/reopened-detail"
        val preview = "$original/variants/source/1/detail.png"
        val loader = FixtureImages(pending = false, grantMillis = 60_000, reusablePermissions = true)
        withViewer(loader, listOf(preview)) {
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { isBluePreview(viewerCenterColor()) }
            compose.waitForIdle()
            compose.onNodeWithText("Открыть оригинал").assertDoesNotExist()
            assertEquals(listOf(preview), loader.requested.toList())

            compose.onNodeWithContentDescription(CLOSE).performClick()
            compose.onNodeWithContentDescription(CLOSE).assertDoesNotExist()
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { isBluePreview(viewerCenterColor()) }
            compose.waitForIdle()
            assertEquals("Reopening must reuse the granted decoded detail variant", listOf(preview), loader.requested.toList())
        }
    }

    @Test
    fun missingNeighborPreviewLoadsItsOriginalOnlyWhenThatPhotoBecomesCurrent() {
        val loader = FixtureImages(pending = false)
        val originals = List(3) { "https://viewer-fixture.invalid/api/v1/check-ins/visit/photos/pending-neighbor-$it" }
        val previews = listOf("${originals[0]}/variants/source/1/detail.png", "",
            "${originals[2]}/variants/source/1/detail.png")
        withViewer(loader, previews, images = PhotoViewerImages(previews, originals)) {
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { loader.requested.isNotEmpty() && loader.active.get() == 0 }
            compose.waitForIdle()
            assertEquals("A pending neighbor must not prefetch its original", listOf(previews[0]), loader.requested.toList())

            compose.onNodeWithContentDescription("Фото 1 из 3").performTouchInput { swipeLeft() }
            compose.waitUntil(10_000) { originals[1] in loader.requested }
            compose.onNodeWithText("2 / 3").assertIsDisplayed()
            assertEquals(setOf(originals[1]), loader.requested.filter { it in originals }.toSet())
            compose.onNodeWithText("Открыть оригинал").assertDoesNotExist()
        }
    }

    @Test
    fun repeatedDismissalCancelsPendingLoadsAndKeepsTheFeedInteractive() {
        val loader = FixtureImages(pending = true)
        withViewer(loader, listOf("https://viewer-fixture.invalid/pending.png")) { fixture ->
            repeat(12) { cycle ->
                compose.onNodeWithText(OPEN).performTouchInput { click() }
                compose.waitUntil(10_000) { loader.active.get() == 1 && loader.requested.size == cycle + 1 }
                compose.onNodeWithContentDescription(CLOSE).performTouchInput { click() }
                compose.waitUntil(10_000) { loader.active.get() == 0 && loader.cancelled.get() == cycle + 1 }
                compose.onNodeWithContentDescription(CLOSE).assertDoesNotExist()

                compose.onNodeWithText(PROBE).performTouchInput { click() }
                compose.waitUntil(5_000) { fixture.clicks.get() == cycle + 1 }
                val before = fixture.scroll.get().value
                compose.onNodeWithTag(SCROLL).performTouchInput {
                    if (cycle % 2 == 0) swipeUp() else swipeDown()
                }
                compose.waitUntil(5_000) { fixture.scroll.get().value != before }
                compose.waitForIdle()
            }
            assertEquals(12, loader.cancelled.get())
            assertEquals(0, loader.active.get())
            assertEquals(12, fixture.clicks.get())
        }
    }

    private fun withViewer(
        loader: FixtureImages,
        urls: List<String>,
        initialIndex: Int = 0,
        images: PhotoViewerImages = PhotoViewerImages(urls),
        block: (ViewerFixture) -> Unit,
    ) {
        val shown = mutableStateOf(false)
        val fixture = ViewerFixture()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(LocalKamelConfig provides loader.config) {
                    CoffeePeekTheme(darkTheme = true) {
                        Surface(Modifier.fillMaxSize()) {
                            Box {
                                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                                    Button(onClick = { shown.value = true }) { Text(OPEN) }
                                    Button(onClick = { fixture.clicks.incrementAndGet() }) { Text(PROBE) }
                                    val scroll = rememberScrollState()
                                    fixture.scroll.set(scroll)
                                    Column(Modifier.fillMaxWidth().weight(1f).testTag(SCROLL).verticalScroll(scroll)) {
                                        repeat(40) { index -> Text("Пост $index", Modifier.height(48.dp)) }
                                    }
                                }
                                if (shown.value) FullScreenImageDialog(images, initialIndex) { shown.value = false }
                            }
                        }
                    }
                }
            } }
            compose.onNodeWithText(OPEN).assertIsDisplayed()
            block(fixture)
        }
    }

    private fun viewerCenterColor(): Int {
        val image = compose.onNodeWithContentDescription("Фото 1 из 1").captureToImage().asAndroidBitmap()
        return image.getPixel(image.width / 2, image.height / 2)
    }

    private fun assertBluePreview(message: String, pixel: Int) {
        // System screenshots can quantize one color channel by one unit.
        assertTrue("$message (ARGB=${Integer.toHexString(pixel)})", isBluePreview(pixel))
    }

    private fun isBluePreview(pixel: Int): Boolean = android.graphics.Color.alpha(pixel) >= 250 &&
            android.graphics.Color.red(pixel) <= 5 && android.graphics.Color.green(pixel) <= 5 &&
            android.graphics.Color.blue(pixel) >= 250

    private class ViewerFixture {
        val clicks = AtomicInteger()
        val scroll = AtomicReference<ScrollState>()
    }

    private class FixtureImages(
        private val pending: Boolean,
        private val pendingAfterFirst: Boolean = false,
        private val grantMillis: Long = 20_000,
        private val reusablePermissions: Boolean = false,
    ) {
        val requested = CopyOnWriteArrayList<String>()
        val inputs = CopyOnWriteArrayList<CustomUrl>()
        val active = AtomicInteger()
        val cancelled = AtomicInteger()
        val config = KamelConfig {
            // Fully local pipeline: no HTTP client, shared cache, or external fixture files.
            fetcher(object : Fetcher<CustomUrl> {
                override val inputDataKClass = CustomUrl::class
                override val source = DataSource.Disk
                override val CustomUrl.isSupported get() = Url(url).host == "viewer-fixture.invalid"
                override fun fetch(data: CustomUrl, resourceConfig: ResourceConfig) = flow {
                    inputs += data
                    requested += data.url
                    val waits = pending || (pendingAfterFirst && requested.size > 1)
                    active.incrementAndGet()
                    try {
                        if (waits) {
                            emit(Resource.Loading(0f))
                            awaitCancellation()
                        } else {
                            val permission = PhotoPermission(
                                SystemPhotoCacheClock.monotonicMillis() + grantMillis,
                                SystemPhotoCacheClock.wallMillis() + grantMillis,
                            )
                            data.displayPermission?.grant(permission, permission.takeIf { reusablePermissions })
                            emit(Resource.Success(ByteReadChannel(byteArrayOf(1)), source))
                        }
                    } finally {
                        active.decrementAndGet()
                        if (waits) cancelled.incrementAndGet()
                    }
                }
            })
            decoder(object : Decoder<ImageBitmap> {
                override val outputKClass = ImageBitmap::class
                override suspend fun decode(channel: ByteReadChannel, resourceConfig: ResourceConfig): ImageBitmap =
                    Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                        .apply { eraseColor(android.graphics.Color.BLUE) }.asImageBitmap()
            })
        }
    }

    private companion object {
        const val OPEN = "Открыть фото"
        const val PROBE = "Проверить ленту"
        const val CLOSE = "Закрыть"
        const val SCROLL = "viewer-underlying-feed"
    }
}
