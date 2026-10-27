package com.coffeepeek.admin.ui.screen.checkins

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Production UI with local fixtures: no account or real check-ins are changed. */
@RunWith(AndroidJUnit4::class)
class CheckInPresentationTest {
    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun calendarRemainsSelectableWithLargeText() {
        val selection = AtomicReference<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                    CoffeePeekTheme(darkTheme = false) {
                        PlatformSystemBars(darkTheme = false)
                        Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                CalendarCard(
                                    VisitedPlacesUiState(calendarMonth = CalendarMonth(2026, 10), selectedDate = "2026-10-14"),
                                    today = "2026-10-14", onPreviousMonth = {}, onNextMonth = {},
                                    onSelectDate = { selection.set(it) }, onToday = { selection.set("2026-10-14") },
                                    homeCounts = mapOf("2026-10-07" to 1, "2026-10-12" to 7, "2026-10-14" to 2),
                                )
                            }
                        }
                    }
                }
            } }
            waitUntil { node { it.contentDescription?.startsWith("12 октября 2026, 7 чекинов") == true } != null }
            screenshot("checkins-calendar-large-text.png").recycle()
            click(node { it.contentDescription?.startsWith("12 октября 2026,") == true }!!)
            waitUntil { selection.get() == "2026-10-12" }
            click(node { it.contentDescription?.startsWith("Сегодня,") == true }!!)
            waitUntil { selection.get() == "2026-10-14" }
        }
    }

    @Test
    fun createChoiceRemainsScrollableWithLargeTextAndShortViewport() {
        val homes = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    CoffeePeekTheme(darkTheme = true) {
                        Surface(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxWidth().statusBarsPadding().height(260.dp)) {
                                CheckInCreateContent(onShop = {}, onHome = { homes.incrementAndGet() })
                            }
                        }
                    }
                }
            } }
            waitUntil { node { it.text?.toString() == "Новый чекин" } != null }
            scrollToText("Дома")
            click(node { it.text?.toString() == "Дома" }!!)
            scrollToText("Продолжить")
            click(node { it.text?.toString() == "Продолжить" }!!)
            waitUntil { homes.get() == 1 }
            screenshot("checkins-create-short-large-text.png").recycle()
        }
    }

    private fun scrollToText(text: String) {
        repeat(8) {
            if (node { it.text?.toString() == text && it.isVisibleToUser } != null) return
            node { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(200)
        }
        assertTrue("Missing action: $text", node { it.text?.toString() == text } != null)
    }

    private fun screenshot(name: String): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val shot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            shot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return shot
    }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            root.refresh()
            if (predicate(root)) return root
            for (index in 0 until root.childCount) root.getChild(index)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun click(label: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Check-in UI did not reach the expected state", condition())
    }

}
