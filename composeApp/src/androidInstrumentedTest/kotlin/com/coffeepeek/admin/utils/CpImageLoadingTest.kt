package com.coffeepeek.admin.utils

import com.coffeepeek.core.media.ImageUrl

import com.coffeepeek.core.designsystem.component.CoffeePeekImage

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import com.coffeepeek.admin.ui.screen.checkins.CalendarCard
import com.coffeepeek.admin.ui.screen.checkins.CalendarMonth
import com.coffeepeek.admin.ui.screen.checkins.CalendarPhoto
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesUiState
import com.coffeepeek.core.media.image.FullScreenPhotoPresence
import com.coffeepeek.core.media.image.PhotoPermission
import com.coffeepeek.core.media.image.PhotoAccessDenied
import com.coffeepeek.core.media.image.SystemPhotoCacheClock
import com.coffeepeek.core.media.image.protectedPhotoPainterCache
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.ResourceConfig
import io.kamel.core.decoder.Decoder
import io.kamel.core.fetcher.Fetcher
import io.kamel.image.config.LocalKamelConfig
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.io.readByteArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Local image pipeline checks: no real network, disk fixtures, or authentication. */
@RunWith(AndroidJUnit4::class)
class CpImageLoadingTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun calendarPhotoStaysVisibleDuringPermissionRenewal() {
        val loader = FixtureImages(grantMillis = 6_000, holdAfterFirst = true)
        fun photoColor(): Int {
            val bitmap = compose.onNodeWithContentDescription("12 октября 2026, 1 чекин", substring = true)
                .captureToImage().asAndroidBitmap()
            return bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        withImageContent(loader, content = {
            CalendarCard(VisitedPlacesUiState(calendarMonth = CalendarMonth(2026, 10)),
                today = "2026-10-20", onPreviousMonth = {}, onNextMonth = {}, onSelectDate = {}, onToday = {},
                homeCounts = mapOf("2026-10-12" to 1),
                homePhotos = mapOf("2026-10-12" to listOf(CalendarPhoto(THUMB))))
        }) {
            compose.waitUntil(10_000) { photoColor() == Color.BLUE }
            compose.waitUntil(10_000) { loader.requested.size == 2 }
            compose.waitForIdle()
            assertEquals("Renewing access must not erase an authorized calendar thumbnail", Color.BLUE, photoColor())
            loader.release.complete(Unit)
            compose.waitUntil(10_000) { loader.decoded.get() == 2 }
            compose.waitUntil(10_000) { photoColor() == Color.BLUE }
        }
    }

    @Test
    fun protectedPhotoRecoversAfterATemporaryRenewalFailure() {
        val loader = FixtureImages(grantMillis = 6_000,
            failedRequests = mapOf(2 to IllegalStateException("Temporary renewal failure")))
        withImageContent(loader, content = {
            CoffeePeekImage(THUMB.replace("/calendar/", "/recovery/"), modifier = Modifier.size(160.dp).testTag(IMAGE))
        }) {
            waitForColor(Color.BLUE)
            compose.waitUntil(12_000) { loader.requested.size >= 3 && loader.decoded.get() >= 2 }
            waitForColor(Color.BLUE)
        }
    }

    @Test
    fun accessDenialImmediatelyRemovesThePreviouslyGrantedPhoto() {
        val loader = FixtureImages(grantMillis = 6_000, failedRequests = mapOf(2 to PhotoAccessDenied()))
        withImageContent(loader, content = {
            CoffeePeekImage(THUMB.replace("/calendar/", "/denied/"), modifier = Modifier.size(160.dp).testTag(IMAGE))
        }) {
            waitForColor(Color.BLUE)
            compose.waitUntil(10_000) { loader.requested.size == 2 && loader.active.get() == 0 }
            compose.waitUntil(2_000) { centerColor() != Color.BLUE }
            assertNull(protectedPhotoPainterCache.get(loader.requested.first(),
                checkNotNull(loader.inputs.first().permissionScope)))
            compose.mainClock.advanceTimeBy(3_000)
            compose.waitForIdle()
            assertEquals("Denied access must not automatically retry or restore an old photo", 2, loader.requested.size)
        }
    }

    @Test
    fun visiblePhotoWaitsForTheDwellBeforeRequestingHigherQuality() {
        val loader = FixtureImages()
        withImageContent(loader, freezeClock = true, content = {
            CoffeePeekImage(PREVIEW, highQualityData = DETAIL, modifier = Modifier.size(160.dp).testTag(IMAGE))
        }) {
            startFrozenContent()
            compose.waitUntil(10_000) { PREVIEW in loader.requested }
            compose.mainClock.advanceTimeBy(1_000)
            assertFalse("A glance must not start a larger transfer", DETAIL in loader.requested)

            compose.mainClock.advanceTimeBy(1_000)
            compose.waitUntil(10_000) { DETAIL in loader.requested }
            compose.mainClock.autoAdvance = true
            waitForColor(Color.GREEN)
            assertEquals(1, loader.requested.count { it == DETAIL })
        }
    }

    @Test
    fun clippedPhotoDoesNotUpgradeUntilMostOfItEntersTheViewport() {
        val loader = FixtureImages()
        val offset = mutableStateOf(120.dp)
        withImageContent(loader, freezeClock = true, content = {
            Box(Modifier.size(160.dp).clipToBounds()) {
                CoffeePeekImage(PREVIEW, highQualityData = DETAIL,
                    modifier = Modifier.offset(y = offset.value).size(160.dp).testTag(IMAGE))
            }
        }) {
            startFrozenContent()
            compose.waitUntil(10_000) { PREVIEW in loader.requested }
            compose.mainClock.advanceTimeBy(3_000)
            assertFalse("A photo with only 25% visible must not upgrade", DETAIL in loader.requested)

            compose.runOnIdle { offset.value = 0.dp }
            startFrozenContent()
            compose.mainClock.advanceTimeBy(1_000)
            assertFalse("Entering the viewport must start a new dwell interval", DETAIL in loader.requested)
            compose.mainClock.advanceTimeBy(1_000)
            compose.waitUntil(10_000) { DETAIL in loader.requested }
            compose.mainClock.autoAdvance = true
            waitForColor(Color.GREEN)
        }
    }

    @Test
    fun pendingHigherQualityTransferKeepsTheDecodedPreview() {
        val loader = FixtureImages(pendingUrls = setOf(DETAIL))
        withImageContent(loader, content = {
            CoffeePeekImage(PREVIEW, highQualityData = DETAIL, upgradeDelayMillis = 0,
                modifier = Modifier.size(160.dp).testTag(IMAGE))
        }) {
            compose.waitUntil(10_000) { loader.requested.containsAll(listOf(PREVIEW, DETAIL)) && loader.active.get() == 1 }
            waitForColor(Color.BLUE)
            assertEquals("Pending larger image must leave the preview in place", Color.BLUE, centerColor())
        }
    }

    @Test
    fun failedHigherQualityTransferKeepsTheDecodedPreview() {
        val loader = FixtureImages(failingUrls = setOf(DETAIL))
        withImageContent(loader, content = {
            CoffeePeekImage(PREVIEW, highQualityData = DETAIL, upgradeDelayMillis = 0,
                modifier = Modifier.size(160.dp).testTag(IMAGE))
        }) {
            compose.waitUntil(10_000) { loader.requested.containsAll(listOf(PREVIEW, DETAIL)) && loader.active.get() == 0 }
            waitForColor(Color.BLUE)
            assertEquals("Failed larger image must leave the preview in place", Color.BLUE, centerColor())
        }
    }

    @Test
    fun identicalPreviewAndQualityUrlsUseOneTransferAcrossRecomposition() {
        val loader = FixtureImages()
        val revision = mutableStateOf(0)
        withImageContent(loader, content = {
            CoffeePeekImage(PREVIEW, highQualityData = PREVIEW, upgradeDelayMillis = 0,
                contentDescription = "Photo ${revision.value}",
                modifier = Modifier.size(160.dp).testTag(IMAGE))
        }) {
            waitForColor(Color.BLUE)
            compose.runOnIdle { revision.value++ }
            compose.waitForIdle()
            assertEquals(listOf(PREVIEW), loader.requested.toList())
            assertEquals(1, loader.decoded.get())
        }
    }

    @Test
    fun prefetchedPhotoIsReusedWhenItIsRenderedAndReopened() {
        val loader = FixtureImages()
        val phase = mutableStateOf(0)
        withImageContent(loader, content = {
            when (phase.value) {
                0 -> PrefetchPhotos(listOf(PREVIEW))
                1, 3 -> CoffeePeekImage(PREVIEW, modifier = Modifier.size(160.dp).testTag(IMAGE))
            }
        }) {
            compose.waitUntil(10_000) { loader.decoded.get() == 1 && loader.active.get() == 0 }
            // Finish the success frame so warming has written the decoded cache before disposal.
            compose.waitForIdle()
            compose.runOnIdle { phase.value = 1 }
            waitForColor(Color.BLUE)
            compose.runOnIdle { phase.value = 2 }
            compose.onNodeWithTag(IMAGE).assertDoesNotExist()
            compose.runOnIdle { phase.value = 3 }
            waitForColor(Color.BLUE)
            assertEquals("Warm/reopen must reuse the same public image cache", listOf(PREVIEW), loader.requested.toList())
            assertEquals(1, loader.decoded.get())
        }
    }

    @Test
    fun simultaneousPrefetchWindowsShareTheTransferBudget() {
        val urls = List(4) { "https://cp-image-fixture.invalid/budget-$it.png" }
        val loader = FixtureImages(pendingUrls = urls.toSet())
        val shown = mutableStateOf(true)
        withImageContent(loader, content = {
            if (shown.value) {
                PrefetchPhotos(urls.take(2))
                PrefetchPhotos(urls.takeLast(2))
            }
        }) {
            compose.waitUntil(10_000) { loader.requested.size >= 2 }
            compose.mainClock.advanceTimeBy(3_000)
            compose.waitForIdle()
            assertEquals("Independent galleries must share two speculative transfers", 2, loader.requested.size)
            assertEquals(2, loader.active.get())
            assertEquals("The combined windows must never exceed the transfer budget", 2, loader.peakActive.get())

            compose.runOnIdle { shown.value = false }
            compose.waitUntil(10_000) { loader.active.get() == 0 }
        }
    }

    @Test
    fun coveredScreenDefersDwellAndPrefetchUntilTheViewerExits() {
        val loader = FixtureImages()
        val coveredNeighbor = "https://cp-image-fixture.invalid/covered-neighbor.png"
        val viewerNeighbor = "https://cp-image-fixture.invalid/viewer-neighbor.png"
        var covered = true
        InstrumentationRegistry.getInstrumentation().runOnMainSync { FullScreenPhotoPresence.enter() }
        try {
            withImageContent(loader, freezeClock = true, content = {
                Box {
                    CoffeePeekImage(PREVIEW, highQualityData = DETAIL,
                        modifier = Modifier.size(160.dp).testTag(IMAGE))
                    PrefetchPhotos(listOf(coveredNeighbor))
                    PrefetchPhotos(listOf(viewerNeighbor), inPhotoViewer = true)
                }
            }) {
                startFrozenContent()
                compose.waitUntil(10_000) { PREVIEW in loader.requested }
                compose.mainClock.advanceTimeBy(3_000)
                startFrozenContent()
                compose.waitUntil(10_000) { viewerNeighbor in loader.requested }
                assertEquals("Only the visible preview and viewer's own neighbor may load while covered",
                    setOf(PREVIEW, viewerNeighbor), loader.requested.toSet())

                compose.runOnIdle {
                    FullScreenPhotoPresence.leave()
                    covered = false
                }
                startFrozenContent()
                compose.mainClock.advanceTimeBy(1_000)
                startFrozenContent()
                compose.waitUntil(10_000) { coveredNeighbor in loader.requested }
                assertFalse("Uncovering the screen must begin a fresh dwell", DETAIL in loader.requested)

                compose.mainClock.advanceTimeBy(1_000)
                compose.waitUntil(10_000) { DETAIL in loader.requested }
                compose.mainClock.autoAdvance = true
                waitForColor(Color.GREEN)
            }
        } finally {
            if (covered) InstrumentationRegistry.getInstrumentation().runOnMainSync { FullScreenPhotoPresence.leave() }
        }
    }

    private fun withImageContent(
        loader: FixtureImages,
        freezeClock: Boolean = false,
        content: @Composable () -> Unit,
        check: () -> Unit,
    ) {
        compose.mainClock.autoAdvance = true
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                // Let App release MainActivity's splash before replacing its onReady effect.
                compose.waitForIdle()
                compose.mainClock.autoAdvance = !freezeClock
                scenario.onActivity { activity -> activity.setContent {
                    CompositionLocalProvider(LocalKamelConfig provides loader.config) {
                        CoffeePeekTheme(darkTheme = true) { Surface { content() } }
                    }
                } }
                check()
            }
        } finally { compose.mainClock.autoAdvance = true }
    }

    private fun startFrozenContent() {
        // First frame lays out the image; the next applies its measured visibility.
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun waitForColor(color: Int) = compose.waitUntil(10_000) { centerColor() == color }

    private fun centerColor(): Int {
        val bitmap = compose.onNodeWithTag(IMAGE).captureToImage().asAndroidBitmap()
        return bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
    }

    private class FixtureImages(
        private val pendingUrls: Set<String> = emptySet(),
        private val failingUrls: Set<String> = emptySet(),
        private val grantMillis: Long = 20_000,
        private val holdAfterFirst: Boolean = false,
        private val failedRequests: Map<Int, Throwable> = emptyMap(),
    ) {
        val release = CompletableDeferred<Unit>()
        val inputs = CopyOnWriteArrayList<ImageUrl>()
        val requested = CopyOnWriteArrayList<String>()
        val active = AtomicInteger()
        val peakActive = AtomicInteger()
        val decoded = AtomicInteger()
        val config = KamelConfig {
            imageBitmapCacheSize = 8
            fetcher(object : Fetcher<ImageUrl> {
                override val inputDataKClass = ImageUrl::class
                override val source = DataSource.Disk
                override val ImageUrl.isSupported get() = Url(url).host == "cp-image-fixture.invalid"
                override fun fetch(data: ImageUrl, resourceConfig: ResourceConfig) = flow {
                    inputs += data
                    requested += data.url
                    val attempt = requested.size
                    val running = active.incrementAndGet()
                    peakActive.updateAndGet { maxOf(it, running) }
                    try {
                        if (holdAfterFirst && attempt > 1) {
                            emit(Resource.Loading(0f))
                            release.await()
                        }
                        when (data.url) {
                            in pendingUrls -> {
                                emit(Resource.Loading(0f))
                                awaitCancellation()
                            }
                            in failingUrls -> emit(Resource.Failure(IllegalStateException("Fixture image unavailable"), source))
                            else -> if (attempt in failedRequests) {
                                emit(Resource.Failure(failedRequests.getValue(attempt), source))
                            } else {
                                val grant = PhotoPermission(SystemPhotoCacheClock.monotonicMillis() + grantMillis,
                                    SystemPhotoCacheClock.wallMillis() + grantMillis)
                                data.displayPermission?.grant(grant, grant)
                                emit(Resource.Success(ByteReadChannel(byteArrayOf(if (data.url == DETAIL) 2 else 1)), source))
                            }
                        }
                    } finally { active.decrementAndGet() }
                }
            })
            decoder(object : Decoder<ImageBitmap> {
                override val outputKClass = ImageBitmap::class
                override suspend fun decode(channel: ByteReadChannel, resourceConfig: ResourceConfig): ImageBitmap {
                    val quality = channel.readRemaining().readByteArray().single().toInt()
                    decoded.incrementAndGet()
                    return Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                        .apply { eraseColor(if (quality == 2) Color.GREEN else Color.BLUE) }.asImageBitmap()
                }
            })
        }
    }

    private companion object {
        const val PREVIEW = "https://cp-image-fixture.invalid/preview.png"
        const val DETAIL = "https://cp-image-fixture.invalid/detail.png"
        const val THUMB = "https://cp-image-fixture.invalid/api/v1/check-ins/visit/photos/calendar/variants/source/1/thumb.png"
        const val IMAGE = "cp-image-loading-fixture"
    }
}
