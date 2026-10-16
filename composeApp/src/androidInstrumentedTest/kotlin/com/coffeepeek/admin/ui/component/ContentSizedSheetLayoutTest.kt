package com.coffeepeek.admin.ui.component

import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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

    @Test
    fun lazyListGrowsFromEmptyAndShrinksAgainWithoutLosingItsFixedHeaderOrFooter() {
        val rowCount = mutableStateOf(0)
        val reportedHeight = AtomicInteger()
        val headerHeight = AtomicInteger()
        val footerHeight = AtomicInteger()
        val footerTop = AtomicInteger()
        val lastRowBottom = AtomicInteger()
        val lazyList = AtomicReference<LazyListState>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme {
                    Box(Modifier.size(width = 240.dp, height = 320.dp)) {
                        ContentSizedSheetLayout(
                            maxContentHeight = 320.dp,
                            onContentHeightChanged = reportedHeight::set,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(Modifier.fillMaxWidth().height(32.dp).onGloballyPositioned {
                                headerHeight.set(it.size.height)
                            }) { Text("Connections") }
                            val listState = rememberLazyListState()
                            lazyList.set(listState)
                            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
                                items(rowCount.value) { index ->
                                    Box(Modifier.fillMaxWidth().height(28.dp).onGloballyPositioned {
                                        if (index == rowCount.value - 1) {
                                            lastRowBottom.set((it.positionInRoot().y + it.size.height).roundToInt())
                                        }
                                    }) { Text("Connection ${index + 1}") }
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(48.dp).onGloballyPositioned {
                                footerHeight.set(it.size.height)
                                footerTop.set(it.positionInRoot().y.roundToInt())
                            }) { Text("Close") }
                        }
                    }
                }
            } }

            waitUntil("An empty lazy list must not reserve a full-height detent") {
                near(reportedHeight.get(), 80 * density) && lazyList.get()?.layoutInfo?.totalItemsCount == 0
            }
            scenario.onActivity { rowCount.value = 40 }
            waitUntil("Loaded connections must receive a finite scroll viewport") {
                near(reportedHeight.get(), 320 * density) && lazyList.get()?.layoutInfo?.totalItemsCount == 40
            }
            scenario.onActivity { lazyList.get().requestScrollToItem(39) }
            waitUntil("The final lazy item must remain reachable above the fixed footer") {
                lazyList.get().layoutInfo.visibleItemsInfo.lastOrNull()?.index == 39 &&
                    abs(lastRowBottom.get() - footerTop.get()) <= 2
            }
            assertTrue("Lazy content must preserve the header", near(headerHeight.get(), 32 * density))
            assertTrue("Lazy content must preserve the footer", near(footerHeight.get(), 48 * density))

            scenario.onActivity { rowCount.value = 0 }
            waitUntil("Clearing loaded connections must shrink the requested native detent") {
                near(reportedHeight.get(), 80 * density) && lazyList.get().layoutInfo.totalItemsCount == 0
            }
            assertTrue("The empty footer must still retain its height", near(footerHeight.get(), 48 * density))
        }
    }

    @Test
    fun fillHeightFormUsesExpandedNativeViewportWithKeyboardPaddingAndRestores() {
        val viewport = mutableStateOf(320.dp)
        val preferredMaximum = mutableStateOf(320.dp)
        val keyboardHeight = mutableStateOf(0.dp)
        val reportedHeight = AtomicInteger()
        val containerHeight = AtomicInteger()
        val containerBottom = AtomicInteger()
        val formHeight = AtomicInteger()
        val formBottom = AtomicInteger()
        val scrollHeight = AtomicInteger()
        val scrollBottom = AtomicInteger()
        val lastRowBottom = AtomicInteger()
        val scroll = AtomicReference<ScrollState>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme {
                    Box(Modifier.size(width = 240.dp, height = viewport.value)) {
                        ContentSizedSheetLayout(
                            maxContentHeight = preferredMaximum.value,
                            onContentHeightChanged = reportedHeight::set,
                            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                                containerHeight.set(it.size.height)
                                containerBottom.set((it.positionInRoot().y + it.size.height).roundToInt())
                            },
                        ) {
                            Box(Modifier.fillMaxSize().onGloballyPositioned {
                                formHeight.set(it.size.height)
                                formBottom.set((it.positionInRoot().y + it.size.height).roundToInt())
                            }) {
                                Column(Modifier.fillMaxSize().padding(bottom = keyboardHeight.value)) {
                                    Box(Modifier.fillMaxWidth().height(32.dp)) { Text("Check-in note") }
                                    val scrollState = rememberScrollState()
                                    scroll.set(scrollState)
                                    Column(
                                        Modifier.weight(1f).onGloballyPositioned {
                                            scrollHeight.set(it.size.height)
                                            scrollBottom.set((it.positionInRoot().y + it.size.height).roundToInt())
                                        }.verticalScroll(scrollState),
                                    ) {
                                        repeat(20) { index ->
                                            Box(Modifier.fillMaxWidth().height(28.dp).onGloballyPositioned {
                                                if (index == 19) {
                                                    lastRowBottom.set((it.positionInRoot().y + it.size.height).roundToInt())
                                                }
                                            }) { Text("Form row ${index + 1}") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } }

            waitUntil("The initial form must fill a bounded native viewport") {
                near(reportedHeight.get(), 320 * density) &&
                    near(containerHeight.get(), 320 * density) &&
                    near(formHeight.get(), 320 * density) &&
                    near(scrollHeight.get(), 288 * density) &&
                    (scroll.get()?.maxValue ?: 0) > 0
            }

            scenario.onActivity {
                viewport.value = 480.dp
                keyboardHeight.value = 160.dp
            }
            waitUntil("Keyboard expansion must fill the actual viewport without an unused region above the keyboard") {
                near(reportedHeight.get(), 320 * density) &&
                    near(containerHeight.get(), 480 * density) &&
                    near(formHeight.get(), 480 * density) &&
                    abs(formBottom.get() - containerBottom.get()) <= 2 &&
                    near(containerBottom.get() - scrollBottom.get(), 160 * density) &&
                    near(scrollHeight.get(), 288 * density) &&
                    (scroll.get()?.maxValue ?: 0) > 0
            }
            scenario.onActivity { scroll.get().dispatchRawDelta(scroll.get().maxValue.toFloat()) }
            waitUntil("The final form row must remain reachable above the simulated keyboard") {
                scroll.get().value == scroll.get().maxValue &&
                    abs(lastRowBottom.get() - scrollBottom.get()) <= 2
            }

            scenario.onActivity { preferredMaximum.value = 280.dp }
            waitUntil("Updating the preferred detent must refresh its reported height while keeping the expanded form filled") {
                near(reportedHeight.get(), 280 * density) &&
                    near(formHeight.get(), 480 * density) &&
                    abs(formBottom.get() - containerBottom.get()) <= 2 &&
                    near(containerBottom.get() - scrollBottom.get(), 160 * density)
            }

            scenario.onActivity {
                viewport.value = 320.dp
                preferredMaximum.value = 320.dp
                keyboardHeight.value = 0.dp
            }
            waitUntil("Dismissing the keyboard must restore the bounded form viewport") {
                near(reportedHeight.get(), 320 * density) &&
                    near(containerHeight.get(), 320 * density) &&
                    near(formHeight.get(), 320 * density) &&
                    near(scrollHeight.get(), 288 * density) &&
                    abs(formBottom.get() - containerBottom.get()) <= 2 &&
                    abs(scrollBottom.get() - containerBottom.get()) <= 2 &&
                    (scroll.get()?.maxValue ?: 0) > 0
            }
            scenario.onActivity { scroll.get().dispatchRawDelta(scroll.get().maxValue.toFloat()) }
            waitUntil("The final form row must remain reachable after the keyboard closes") {
                scroll.get().value == scroll.get().maxValue &&
                    abs(lastRowBottom.get() - scrollBottom.get()) <= 2
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
