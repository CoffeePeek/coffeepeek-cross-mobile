package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the actual Material modal window, including its drag handle and bottom anchor. */
@RunWith(AndroidJUnit4::class)
class SwipeDismissModalBottomSheetTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val density get() = instrumentation.targetContext.resources.displayMetrics.density

    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun shortContentWrapsBelowTheHeightLimitAndStaysAtTheWindowBottom() = withSheet(.9f, 2) { _, _, measurements ->
        waitUntil("A short modal must wrap its content and remain anchored to the window bottom") {
            near(measurements.body.get().height, 136 * density) && measurements.isBottomAnchored()
        }
        assertTrue("The height fraction is a maximum, not a request for blank space",
            measurements.totalHeight() < measurements.windowHeight.get() * .5f)
        assertTrue("Short content must not scroll", measurements.scroll.get().maxValue == 0)
        screenshot("modal-short-content.png")
    }

    @Test
    fun longContentKeepsTheHeaderAndFooterVisibleAndTheFinalRowReachable() = withSheet(.76f, 60) { scenario, _, measurements ->
        waitUntil("A long modal must respect its height limit and the bottom anchor") {
            near(measurements.totalHeight(), measurements.windowHeight.get() * .76f) &&
                measurements.isBottomAnchored() && measurements.scroll.get()?.maxValue?.let { it > 0 } == true
        }
        val headerBeforeScroll = measurements.header.get()
        val footerBeforeScroll = measurements.footer.get()
        assertTrue("The fixed header must retain its height", near(headerBeforeScroll.height, 32 * density))
        assertTrue("The fixed footer must retain its height", near(footerBeforeScroll.height, 48 * density))
        assertTrue("The scroll viewport must end above the footer",
            near(measurements.viewport.get().bottom, footerBeforeScroll.top))

        scenario.onActivity {
            measurements.scroll.get().let { it.dispatchRawDelta(it.maxValue.toFloat()) }
        }
        waitUntil("The final row must be reachable without covering the footer") {
            measurements.scroll.get().value == measurements.scroll.get().maxValue &&
                near(measurements.lastRowBottom.get().toFloat(), measurements.viewport.get().bottom)
        }
        assertTrue("Scrolling must leave the header fixed", near(measurements.header.get().top, headerBeforeScroll.top))
        assertTrue("Scrolling must leave the footer fixed", near(measurements.footer.get().top, footerBeforeScroll.top))
        assertTrue("Scrolling must preserve the bottom anchor", measurements.isBottomAnchored())
        screenshot("modal-long-content-scrolled.png")
    }

    @Test
    fun replacingLongContentWithShortContentShrinksTheOpenModal() = withSheet(.76f, 60) { scenario, rows, measurements ->
        waitUntil("The initial long modal must fill its bounded height") {
            near(measurements.totalHeight(), measurements.windowHeight.get() * .76f) && measurements.isBottomAnchored()
        }
        val originalTop = measurements.body.get().top
        scenario.onActivity { rows.value = 2 }
        waitUntil("An already open modal must shrink after its content becomes short") {
            near(measurements.body.get().height, 136 * density) &&
                measurements.isBottomAnchored() && measurements.scroll.get()?.maxValue == 0
        }
        assertTrue("Shrinking must move the top down while keeping the bottom fixed", measurements.body.get().top > originalTop)
        screenshot("modal-long-to-short.png")
    }

    private fun withSheet(
        fraction: Float,
        initialRows: Int,
        block: (ActivityScenario<MainActivity>, MutableState<Int>, SheetMeasurements) -> Unit,
    ) {
        val rows = mutableStateOf(initialRows)
        val measurements = SheetMeasurements()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = true) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Text("Underlying page")
                        SwipeDismissModalBottomSheet(onDismissRequest = {}, maxSheetHeightFraction = fraction) {
                            val windowHeight = LocalWindowInfo.current.containerSize.height
                            val bottomInset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
                            Column(Modifier.fillMaxWidth().onGloballyPositioned {
                                measurements.windowHeight.set(windowHeight)
                                measurements.bottomInset.set(bottomInset)
                                measurements.body.set(it.unclippedBoundsInWindow())
                            }) {
                                Box(Modifier.fillMaxWidth().height(32.dp).onGloballyPositioned {
                                    measurements.header.set(it.unclippedBoundsInWindow())
                                }) { Text("Sheet title") }
                                val scroll = rememberScrollState()
                                measurements.scroll.set(scroll)
                                Column(
                                    Modifier.weight(1f, fill = false).fillMaxWidth().onGloballyPositioned {
                                        measurements.viewport.set(it.unclippedBoundsInWindow())
                                    }.verticalScroll(scroll),
                                ) {
                                    val rowCount = rows.value
                                    repeat(rowCount) { index ->
                                        Box(Modifier.fillMaxWidth().height(28.dp).onGloballyPositioned {
                                            if (index == rowCount - 1) {
                                                measurements.lastRowBottom.set(it.unclippedBoundsInWindow().bottom.roundToInt())
                                            }
                                        }) { Text("Option ${index + 1}") }
                                    }
                                }
                                Box(Modifier.fillMaxWidth().height(48.dp).onGloballyPositioned {
                                    measurements.footer.set(it.unclippedBoundsInWindow())
                                }) { Text("Continue") }
                            }
                        }
                    }
                }
            } }
            block(scenario, rows, measurements)
        }
    }

    private inner class SheetMeasurements {
        val windowHeight = AtomicInteger()
        val bottomInset = AtomicInteger()
        val body = AtomicReference(Rect.Zero)
        val header = AtomicReference(Rect.Zero)
        val viewport = AtomicReference(Rect.Zero)
        val footer = AtomicReference(Rect.Zero)
        val lastRowBottom = AtomicInteger()
        val scroll = AtomicReference<ScrollState>()

        fun totalHeight() = body.get().height + 44 * density + bottomInset.get()
        fun isBottomAnchored() = windowHeight.get() > 0 &&
            near(body.get().bottom + bottomInset.get(), windowHeight.get().toFloat())
    }

    private fun LayoutCoordinates.unclippedBoundsInWindow(): Rect {
        val origin = positionInWindow()
        return Rect(origin.x, origin.y, origin.x + size.width, origin.y + size.height)
    }

    private fun near(actual: Float, expected: Float) = abs(actual - expected) <= 2

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        instrumentation.waitForIdleSync()
        assertTrue(message, condition())
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.filesDir, "sheet-layout").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
