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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
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
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.ResourceConfig
import io.kamel.core.config.stringMapper
import io.kamel.core.decoder.Decoder
import io.kamel.core.fetcher.Fetcher
import io.kamel.image.config.LocalKamelConfig
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
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
    fun openingMiddlePhotoLoadsOnlyThatPageAndSwipingLoadsTheNext() {
        val loader = FixtureImages(pending = false)
        val urls = List(5) { "https://viewer-fixture.invalid/photo-$it.png" }
        withViewer(loader, urls, initialIndex = 2) { _ ->
            compose.onNodeWithText(OPEN).performClick()
            compose.waitUntil(10_000) { loader.requested.isNotEmpty() && loader.active.get() == 0 }
            compose.onNodeWithText("3 / 5").assertIsDisplayed()
            compose.waitForIdle()
            assertEquals("Opening must not eagerly fetch both neighboring pages", listOf(urls[2]), loader.requested.toList())

            compose.onNodeWithContentDescription("Фото 3 из 5").performTouchInput { swipeLeft() }
            compose.waitUntil(10_000) { urls[3] in loader.requested }
            compose.onNodeWithText("4 / 5").assertIsDisplayed()
            compose.onNodeWithContentDescription(CLOSE).performClick()
            compose.onNodeWithContentDescription(CLOSE).assertDoesNotExist()
            compose.onNodeWithText(PROBE).performTouchInput { click() }
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
                                if (shown.value) FullScreenImageDialog(urls, initialIndex) { shown.value = false }
                            }
                        }
                    }
                }
            } }
            compose.onNodeWithText(OPEN).assertIsDisplayed()
            block(fixture)
        }
    }

    private class ViewerFixture {
        val clicks = AtomicInteger()
        val scroll = AtomicReference<ScrollState>()
    }

    private class FixtureImages(private val pending: Boolean) {
        val requested = CopyOnWriteArrayList<String>()
        val active = AtomicInteger()
        val cancelled = AtomicInteger()
        val config = KamelConfig {
            // Fully local pipeline: no HTTP client, shared cache, or external fixture files.
            stringMapper()
            fetcher(object : Fetcher<Url> {
                override val inputDataKClass = Url::class
                override val source = DataSource.Disk
                override val Url.isSupported get() = host == "viewer-fixture.invalid"
                override fun fetch(data: Url, resourceConfig: ResourceConfig) = flow {
                    requested += data.toString()
                    active.incrementAndGet()
                    try {
                        if (pending) {
                            emit(Resource.Loading(0f))
                            awaitCancellation()
                        } else {
                            emit(Resource.Success(ByteReadChannel(byteArrayOf(1)), source))
                        }
                    } finally {
                        active.decrementAndGet()
                        if (pending) cancelled.incrementAndGet()
                    }
                }
            })
            decoder(object : Decoder<ImageBitmap> {
                override val outputKClass = ImageBitmap::class
                override suspend fun decode(channel: ByteReadChannel, resourceConfig: ResourceConfig): ImageBitmap =
                    Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).asImageBitmap()
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
