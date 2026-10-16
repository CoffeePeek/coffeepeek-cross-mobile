package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.screen.review.ConsumedDrinkField
import com.coffeepeek.domain.model.ConsumedDrinkOption
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real menus, including placement and scrolling, without network data. */
@RunWith(AndroidJUnit4::class)
class AnchoredMenuInteractionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before
    fun grantStartupPermissions() {
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.grantRuntimePermission(packageName, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    fun checkInActionsStayBesideTheirTriggerAndDismissAfterSelection() {
        val reports = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = true) {
                    Surface(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().statusBarsPadding().padding(24.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            CheckInActions(onEditClick = null, onReportClick = { reports.incrementAndGet() })
                        }
                    }
                }
            } }
            val trigger = waitFor { it.contentDescription?.toString() == "Действия с чекином" }
            val triggerBounds = clickTarget(trigger).bounds()
            click(trigger)
            val report = waitFor { it.text?.toString() == "Пожаловаться" }
            val actionBounds = clickTarget(report).bounds()
            val density = instrumentation.targetContext.resources.displayMetrics.density
            screenshot("checkin-actions-anchored.png")
            assertTrue("Menu must stay beside the pressed button: $triggerBounds / $actionBounds",
                actionBounds.top <= triggerBounds.bottom + 96 * density)
            assertTrue("Menu must align with the trigger horizontally: $triggerBounds / $actionBounds",
                actionBounds.right >= triggerBounds.left)
            assertTrue("An unavailable action must not appear", node { it.text?.toString() == "Редактировать" } == null)
            click(report)
            waitUntil { reports.get() == 1 && node { it.text?.toString() == "Пожаловаться" } == null }
            click(waitFor { it.contentDescription?.toString() == "Действия с чекином" })
            waitFor { it.text?.toString() == "Пожаловаться" }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            waitUntil { node { it.text?.toString() == "Пожаловаться" } == null }
            assertEquals("Dismissal must not invoke an action", 1, reports.get())
        }
    }

    @Test
    fun longDrinkMenuHasBoundedHeightAndReachesTheLastOption() {
        val selection = AtomicReference<String?>()
        val drinks = (1..24).map { ConsumedDrinkOption("drink-$it", "Напиток $it", "Drink $it") }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var slug by remember { mutableStateOf<String?>(null) }
                CoffeePeekTheme(darkTheme = true) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.statusBarsPadding().padding(24.dp)) {
                            ConsumedDrinkField(
                                drinks = drinks, slug = slug, customName = null, savedName = null,
                                error = null, onRetry = {},
                                onChange = { value, _ -> slug = value; selection.set(value) },
                            )
                            Text("Выбрано: ${slug.orEmpty()}")
                        }
                    }
                }
            } }
            click(waitFor { it.text?.toString() == "Не выбран" })
            waitFor { it.text?.toString() in setOf("Напиток 1", "Drink 1") }
            val scrollable = waitFor { it.isScrollable }
            val density = instrumentation.targetContext.resources.displayMetrics.density
            assertTrue("A long drink menu must not cover the entire screen: ${scrollable.bounds()}",
                scrollable.bounds().height() <= 360 * density)
            screenshot("drink-menu-compact.png")
            repeat(12) {
                if (node { it.text?.toString() in setOf("Напиток 24", "Drink 24") && it.isVisibleToUser } == null) {
                    node { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    instrumentation.waitForIdleSync()
                    Thread.sleep(100)
                }
            }
            click(waitFor { it.text?.toString() in setOf("Напиток 24", "Drink 24") && it.isVisibleToUser })
            waitUntil { selection.get() == "drink-24" }
            waitFor { it.text?.toString() == "Выбрано: drink-24" }
            waitUntil { node { it.isScrollable } == null }
        }
    }

    private fun AccessibilityNodeInfo.bounds() = Rect().also { getBoundsInScreen(it) }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(root)) return root
            for (index in 0 until root.childCount) root.getChild(index)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun waitFor(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        waitUntil { node(predicate) != null }
        return requireNotNull(node(predicate))
    }

    private fun click(label: AccessibilityNodeInfo) {
        assertTrue(clickTarget(label).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun clickTarget(label: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        return requireNotNull(target) { "No clickable control for ${label.text ?: label.contentDescription}" }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Menu did not reach the expected state", condition())
    }
}
