package com.coffeepeek.admin.ui.component

import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android hosts the common measurement primitive used by the iOS sheet. These tests do not
 * replace Android's sheet or exercise a native UIKit presentation.
 */
@RunWith(AndroidJUnit4::class)
class ContentSizedSheetLayoutTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val density get() = instrumentation.targetContext.resources.displayMetrics.density

    @Before
    fun grantStartupPermissions() {
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.grantRuntimePermission(packageName, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    fun naturalHeightDoesNotDependOnTheCurrentNativeViewport() {
        val viewport = mutableStateOf(64.dp)
        val reportedHeight = AtomicInteger()
        val containerHeight = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme {
                    Box(Modifier.size(width = 240.dp, height = viewport.value)) {
                        ContentSizedSheetLayout(
                            maxContentHeight = 320.dp,
                            onContentHeightChanged = reportedHeight::set,
                            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                                containerHeight.set(it.size.height)
                            },
                        ) {
                            Spacer(Modifier.height(40.dp))
                            Spacer(Modifier.height(48.dp))
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                }
            } }

            waitUntil("Short viewport must still measure the complete content") {
                near(reportedHeight.get(), 136 * density) && near(containerHeight.get(), 64 * density)
            }
            val naturalHeight = reportedHeight.get()
            scenario.onActivity { viewport.value = 280.dp }
            waitUntil("The simulated native detent must expand") { near(containerHeight.get(), 280 * density) }
            assertEquals("A larger viewport must not add blank space to the requested height", naturalHeight, reportedHeight.get())

            scenario.onActivity { viewport.value = 64.dp }
            waitUntil("The simulated native detent must shrink") { near(containerHeight.get(), 64 * density) }
            assertEquals("A smaller viewport must not truncate the requested height", naturalHeight, reportedHeight.get())
        }
    }

    @Test
    fun weightedContentFitsShortSheetsAndLongOrFillContentRemainsBounded() {
        val mode = mutableStateOf(ContentMode.Short)
        val reportedHeight = AtomicInteger()
        val scroll = AtomicReference<ScrollState>()
        val footerHeight = AtomicInteger()
        val footerTop = AtomicInteger()
        val lastRowBottom = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme {
                    Box(Modifier.size(width = 240.dp, height = 320.dp)) {
                        ContentSizedSheetLayout(
                            maxContentHeight = 320.dp,
                            onContentHeightChanged = reportedHeight::set,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (mode.value == ContentMode.Fill) {
                                // Existing nested forms explicitly request all available height.
                                Column(Modifier.fillMaxSize()) { Text("Full-height form") }
                            } else {
                                Box(Modifier.fillMaxWidth().height(32.dp)) { Text("Sheet title") }
                                val scrollState = rememberScrollState()
                                scroll.set(scrollState)
                                Column(Modifier.weight(1f).verticalScroll(scrollState)) {
                                    val rowCount = if (mode.value == ContentMode.Long) 20 else 2
                                    repeat(rowCount) { index ->
                                        Box(Modifier.fillMaxWidth().height(28.dp).onGloballyPositioned {
                                            if (index == rowCount - 1) {
                                                lastRowBottom.set((it.positionInRoot().y + it.size.height).roundToInt())
                                            }
                                        }) { Text("Option ${index + 1}") }
                                    }
                                }
                                Box(Modifier.fillMaxWidth().height(48.dp).onGloballyPositioned {
                                    footerHeight.set(it.size.height)
                                    footerTop.set(it.positionInRoot().y.roundToInt())
                                }) { Text("Continue") }
                            }
                        }
                    }
                }
            } }

            waitUntil("Default weight must fit short content instead of forcing a full-height sheet") {
                near(reportedHeight.get(), 136 * density) && scroll.get()?.maxValue == 0
            }
            assertTrue("A fixed footer must retain its height", near(footerHeight.get(), 48 * density))

            scenario.onActivity { mode.value = ContentMode.Long }
            waitUntil("Long scroll content must be bounded by the available sheet height") {
                near(reportedHeight.get(), 320 * density) && (scroll.get()?.maxValue ?: 0) > 0
            }
            scenario.onActivity { scroll.get().dispatchRawDelta(scroll.get().maxValue.toFloat()) }
            waitUntil("The final part of a long sheet must remain reachable") {
                scroll.get().value == scroll.get().maxValue &&
                    abs(lastRowBottom.get() - footerTop.get()) <= 2
            }
            assertTrue("Scrollable content must leave room for the fixed footer", near(footerHeight.get(), 48 * density))

            scenario.onActivity { mode.value = ContentMode.Short }
            waitUntil("Replacing long content must shrink the requested detent again") {
                near(reportedHeight.get(), 136 * density) && scroll.get()?.maxValue == 0
            }

            scenario.onActivity { mode.value = ContentMode.Fill }
            waitUntil("Explicit fill content must have finite constraints") { near(reportedHeight.get(), 320 * density) }
            scenario.onActivity { mode.value = ContentMode.Short }
            waitUntil("Replacing a fill-height form must also shrink the requested detent") {
                near(reportedHeight.get(), 136 * density) && scroll.get()?.maxValue == 0
            }
        }
    }

    private fun near(actual: Int, expected: Float) = abs(actual - expected.roundToInt()) <= 2

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        instrumentation.waitForIdleSync()
        assertTrue(message, condition())
    }

    private enum class ContentMode { Short, Long, Fill }
}
